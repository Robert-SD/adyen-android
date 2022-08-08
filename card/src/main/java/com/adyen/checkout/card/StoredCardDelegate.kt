/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 26/7/2022.
 */

package com.adyen.checkout.card

import com.adyen.checkout.card.api.model.Brand
import com.adyen.checkout.card.data.CardType
import com.adyen.checkout.card.data.DetectedCardType
import com.adyen.checkout.card.data.ExpiryDate
import com.adyen.checkout.card.util.AddressValidationUtils
import com.adyen.checkout.card.util.CardValidationUtils
import com.adyen.checkout.card.util.InstallmentUtils
import com.adyen.checkout.card.util.KcpValidationUtils
import com.adyen.checkout.components.model.paymentmethods.StoredPaymentMethod
import com.adyen.checkout.components.model.payments.request.CardPaymentMethod
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.repository.PublicKeyRepository
import com.adyen.checkout.components.ui.FieldState
import com.adyen.checkout.components.ui.Validation
import com.adyen.checkout.components.util.PaymentMethodTypes
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.exception.ComponentException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.cse.CardEncrypter
import com.adyen.checkout.cse.EncryptedCard
import com.adyen.checkout.cse.UnencryptedCard
import com.adyen.checkout.cse.exception.EncryptionException
import com.adyen.threeds2.ThreeDS2Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class StoredCardDelegate(
    private val storedPaymentMethod: StoredPaymentMethod,
    private val configuration: CardConfiguration,
    private val cardEncrypter: CardEncrypter,
    private val publicKeyRepository: PublicKeyRepository,
) : CardDelegate {

    override val inputData: CardInputData = CardInputData()

    private val _outputDataFlow = MutableStateFlow<CardOutputData?>(null)
    override val outputDataFlow: Flow<CardOutputData?> = _outputDataFlow

    private val _componentStateFlow = MutableStateFlow<CardComponentState?>(null)
    override val componentStateFlow: Flow<CardComponentState?> = _componentStateFlow

    private val _exceptionFlow = MutableSharedFlow<CheckoutException>(0, 1, BufferOverflow.DROP_OLDEST)
    override val exceptionFlow: Flow<CheckoutException> = _exceptionFlow

    private val outputData
        get() = _outputDataFlow.value

    private val noCvcBrands: Set<CardType> = hashSetOf(CardType.BCMC)

    private var publicKey: String? = null

    private var coroutineScope: CoroutineScope? = null

    private val cardType = CardType.getByBrandName(storedPaymentMethod.brand.orEmpty())
    private val storedDetectedCardTypes = cardType?.let {
        DetectedCardType(
            cardType,
            isReliable = true,
            enableLuhnCheck = true,
            cvcPolicy = when {
                configuration.isHideCvcStoredCard || noCvcBrands.contains(cardType) -> Brand.FieldPolicy.HIDDEN
                else -> Brand.FieldPolicy.REQUIRED
            },
            expiryDatePolicy = Brand.FieldPolicy.REQUIRED,
            isSupported = true
        )
    }

    override fun initialize(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
        initializeInputData()
        fetchPublicKey()
    }

    private fun fetchPublicKey() {
        coroutineScope?.launch {
            publicKeyRepository.fetchPublicKey(
                environment = configuration.environment,
                clientKey = configuration.clientKey
            ).fold(
                onSuccess = { key ->
                    publicKey = key
                    outputData?.let { createComponentState(it) }
                },
                onFailure = { e ->
                    _exceptionFlow.tryEmit(ComponentException("Unable to fetch publicKey.", e))
                }
            )
        }
    }

    override fun onInputDataChanged(inputData: CardInputData) {
        Logger.v(TAG, "onInputDataChanged")

        val detectedCardTypes = storedDetectedCardTypes

        _outputDataFlow.tryEmit(
            makeOutputData(
                cardNumber = inputData.cardNumber,
                expiryDate = inputData.expiryDate,
                securityCode = inputData.securityCode,
                holderName = inputData.holderName,
                socialSecurityNumber = inputData.socialSecurityNumber,
                kcpBirthDateOrTaxNumber = inputData.kcpBirthDateOrTaxNumber,
                kcpCardPassword = inputData.kcpCardPassword,
                addressInputModel = inputData.address,
                isStorePaymentSelected = inputData.isStorePaymentSelected,
                detectedCardType = detectedCardTypes,
                selectedInstallmentOption = inputData.installmentOption,
            )
        )
        outputData?.let {
            createComponentState(it)
        }
    }

    override fun getPaymentMethodType(): String {
        return storedPaymentMethod.type ?: PaymentMethodTypes.UNKNOWN
    }

    override fun createComponentState(outputData: CardOutputData) {
        Logger.v(TAG, "createComponentState")

        val cardNumber = outputData.cardNumberState.value

        val firstCardType = outputData.detectedCardTypes.firstOrNull()?.cardType

        val binValue = cardNumber.take(BIN_VALUE_LENGTH)

        val publicKey = publicKey

        // If data is not valid we just return empty object, encryption would fail and we don't pass unencrypted data.
        if (!outputData.isValid || publicKey == null) {
            _componentStateFlow.tryEmit(
                CardComponentState(
                    paymentComponentData = PaymentComponentData(),
                    isInputValid = outputData.isValid,
                    isReady = publicKey != null,
                    cardType = firstCardType,
                    binValue = binValue,
                    lastFourDigits = null
                )
            )
            return
        }

        val unencryptedCardBuilder = UnencryptedCard.Builder()

        val encryptedCard: EncryptedCard = try {
            if (!isCvcHidden()) {
                val cvc = outputData.securityCodeState.value
                if (cvc.isNotEmpty()) unencryptedCardBuilder.setCvc(cvc)
            }
            val expiryDateResult = outputData.expiryDateState.value
            if (expiryDateResult != ExpiryDate.EMPTY_DATE) {
                unencryptedCardBuilder.setExpiryMonth(expiryDateResult.expiryMonth.toString())
                unencryptedCardBuilder.setExpiryYear(expiryDateResult.expiryYear.toString())
            }

            cardEncrypter.encryptFields(unencryptedCardBuilder.build(), publicKey)
        } catch (e: EncryptionException) {
            _exceptionFlow.tryEmit(e)
            _componentStateFlow.tryEmit(
                CardComponentState(
                    paymentComponentData = PaymentComponentData(),
                    isInputValid = false,
                    isReady = true,
                    cardType = firstCardType,
                    binValue = binValue,
                    lastFourDigits = null
                )
            )
            return
        }

        _componentStateFlow.tryEmit(
            mapComponentState(
                encryptedCard,
                outputData,
                cardNumber,
                firstCardType,
                binValue
            )
        )
    }

    private fun validateSecurityCode(securityCode: String, cardType: DetectedCardType?): FieldState<String> {
        return if (configuration.isHideCvcStoredCard || noCvcBrands.contains(cardType?.cardType)) {
            FieldState(
                securityCode,
                Validation.Valid
            )
        } else {
            CardValidationUtils.validateSecurityCode(securityCode, cardType)
        }
    }

    private fun isCvcHidden(): Boolean {
        return configuration.isHideCvcStoredCard || noCvcBrands.contains(cardType)
    }

    override fun requiresInput(): Boolean {
        return !configuration.isHideCvcStoredCard
    }

    override fun isHolderNameRequired(): Boolean {
        return false
    }

    override fun showStorePaymentField(): Boolean {
        return configuration.isStorePaymentFieldVisible
    }

    override fun getKcpBirthDateOrTaxNumberHint(input: String): Int {
        return when {
            input.length > KcpValidationUtils.KCP_BIRTH_DATE_LENGTH -> R.string.checkout_kcp_tax_number_hint
            else -> R.string.checkout_kcp_birth_date_or_tax_number_hint
        }
    }

    private fun getSupportedCardTypes(): List<CardType> = emptyList()

    override fun isDualBrandedFlow(cardOutputData: CardOutputData): Boolean {
        val reliableDetectedCards = cardOutputData.detectedCardTypes.filter { it.isReliable }
        return reliableDetectedCards.size > 1 && reliableDetectedCards.any { it.isSelected }
    }

    override fun isInstallmentsRequired(cardOutputData: CardOutputData): Boolean {
        return cardOutputData.installmentOptions.isNotEmpty()
    }

    private fun mapComponentState(
        encryptedCard: EncryptedCard,
        stateOutputData: CardOutputData,
        cardNumber: String,
        firstCardType: CardType?,
        binValue: String
    ): CardComponentState {
        val cardPaymentMethod = CardPaymentMethod().apply {
            type = CardPaymentMethod.PAYMENT_METHOD_TYPE

            storedPaymentMethodId = getPaymentMethodId()

            if (!isCvcHidden()) {
                encryptedSecurityCode = encryptedCard.encryptedSecurityCode
            }

            if (isDualBrandedFlow(stateOutputData)) {
                brand = stateOutputData.detectedCardTypes.first { it.isSelected }.cardType.txVariant
            }

            try {
                threeDS2SdkVersion = ThreeDS2Service.INSTANCE.sdkVersion
            } catch (e: ClassNotFoundException) {
                Logger.e(TAG, "threeDS2SdkVersion not set because 3DS2 SDK is not present in project.")
            } catch (e: NoClassDefFoundError) {
                Logger.e(TAG, "threeDS2SdkVersion not set because 3DS2 SDK is not present in project.")
            }
        }

        val paymentComponentData = makePaymentComponentData(cardPaymentMethod, stateOutputData)

        val lastFour = cardNumber.takeLast(LAST_FOUR_LENGTH)

        return CardComponentState(
            paymentComponentData = paymentComponentData,
            isInputValid = true,
            isReady = true,
            cardType = firstCardType,
            binValue = binValue,
            lastFourDigits = lastFour
        )
    }

    private fun makePaymentComponentData(
        cardPaymentMethod: CardPaymentMethod,
        stateOutputData: CardOutputData
    ): PaymentComponentData<CardPaymentMethod> {
        return PaymentComponentData<CardPaymentMethod>().apply {
            paymentMethod = cardPaymentMethod
            storePaymentMethod = stateOutputData.isStoredPaymentMethodEnable
            shopperReference = configuration.shopperReference
            if (isInstallmentsRequired(stateOutputData)) {
                installments = InstallmentUtils.makeInstallmentModelObject(stateOutputData.installmentState.value)
            }
        }
    }

    private fun initializeInputData() {
        inputData.cardNumber = storedPaymentMethod.lastFour.orEmpty()

        try {
            val storedDate = ExpiryDate(
                storedPaymentMethod.expiryMonth.orEmpty().toInt(),
                storedPaymentMethod.expiryYear.orEmpty().toInt()
            )
            inputData.expiryDate = storedDate
        } catch (e: NumberFormatException) {
            Logger.e(TAG, "Failed to parse stored Date", e)
            inputData.expiryDate = ExpiryDate.EMPTY_DATE
        }

        if (!requiresInput()) {
            onInputDataChanged(inputData)
        }
    }

    @Suppress("LongParameterList")
    private fun makeOutputData(
        cardNumber: String,
        expiryDate: ExpiryDate,
        securityCode: String,
        holderName: String,
        socialSecurityNumber: String,
        kcpBirthDateOrTaxNumber: String,
        kcpCardPassword: String,
        addressInputModel: AddressInputModel,
        isStorePaymentSelected: Boolean,
        detectedCardType: DetectedCardType?,
        selectedInstallmentOption: InstallmentModel?
    ): CardOutputData {
        return CardOutputData(
            cardNumberState = FieldState(cardNumber, Validation.Valid),
            expiryDateState = FieldState(expiryDate, Validation.Valid),
            securityCodeState = validateSecurityCode(securityCode, detectedCardType),
            holderNameState = FieldState(holderName, Validation.Valid),
            socialSecurityNumberState = FieldState(socialSecurityNumber, Validation.Valid),
            kcpBirthDateOrTaxNumberState = FieldState(kcpBirthDateOrTaxNumber, Validation.Valid),
            kcpCardPasswordState = FieldState(kcpCardPassword, Validation.Valid),
            addressState = AddressValidationUtils.makeValidEmptyAddressOutput(addressInputModel),
            installmentState = FieldState(selectedInstallmentOption, Validation.Valid),
            isStoredPaymentMethodEnable = isStorePaymentSelected,
            cvcUIState = makeCvcUIState(detectedCardType?.cvcPolicy),
            expiryDateUIState = makeExpiryDateUIState(detectedCardType?.expiryDatePolicy),
            detectedCardTypes = listOfNotNull(detectedCardType),
            isSocialSecurityNumberRequired = false,
            isKCPAuthRequired = false,
            addressUIState = AddressFormUIState.NONE,
            installmentOptions = emptyList(),
            countryOptions = emptyList(),
            stateOptions = emptyList(),
            supportedCardTypes = getSupportedCardTypes(),
        )
    }

    private fun makeCvcUIState(cvcPolicy: Brand.FieldPolicy?): InputFieldUIState {
        Logger.d(TAG, "makeCvcUIState: $cvcPolicy")
        return when {
            isCvcHidden() -> InputFieldUIState.HIDDEN
            // We treat CvcPolicy.HIDDEN as OPTIONAL for now to avoid hiding and showing the cvc field while the user
            // is typing the card number.
            cvcPolicy == Brand.FieldPolicy.OPTIONAL
                || cvcPolicy == Brand.FieldPolicy.HIDDEN -> InputFieldUIState.OPTIONAL
            else -> InputFieldUIState.REQUIRED
        }
    }

    private fun makeExpiryDateUIState(expiryDatePolicy: Brand.FieldPolicy?): InputFieldUIState {
        return when (expiryDatePolicy) {
            Brand.FieldPolicy.OPTIONAL, Brand.FieldPolicy.HIDDEN -> InputFieldUIState.OPTIONAL
            else -> InputFieldUIState.REQUIRED
        }
    }

    private fun getPaymentMethodId(): String {
        return storedPaymentMethod.id ?: "ID_NOT_FOUND"
    }

    override fun clear() {
        this.coroutineScope = null
    }

    companion object {
        private val TAG = LogUtil.getTag()
        private const val BIN_VALUE_LENGTH = 6
        private const val LAST_FOUR_LENGTH = 4
    }
}
