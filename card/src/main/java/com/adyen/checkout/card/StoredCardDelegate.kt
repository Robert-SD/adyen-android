/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 26/7/2022.
 */

package com.adyen.checkout.card

import androidx.annotation.VisibleForTesting
import com.adyen.checkout.card.api.model.Brand
import com.adyen.checkout.card.data.CardType
import com.adyen.checkout.card.data.DetectedCardType
import com.adyen.checkout.card.data.ExpiryDate
import com.adyen.checkout.card.util.AddressValidationUtils
import com.adyen.checkout.card.util.CardValidationUtils
import com.adyen.checkout.components.flow.MutableSingleEventSharedFlow
import com.adyen.checkout.components.model.paymentmethods.StoredPaymentMethod
import com.adyen.checkout.components.model.payments.request.CardPaymentMethod
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.repository.PublicKeyRepository
import com.adyen.checkout.components.ui.ComponentMode
import com.adyen.checkout.components.ui.FieldState
import com.adyen.checkout.components.ui.Validation
import com.adyen.checkout.components.ui.ViewProvider
import com.adyen.checkout.components.ui.view.ComponentViewType
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal class StoredCardDelegate(
    private val storedPaymentMethod: StoredPaymentMethod,
    override val configuration: CardConfiguration,
    private val cardEncrypter: CardEncrypter,
    private val publicKeyRepository: PublicKeyRepository,
) : CardDelegate {

    private val noCvcBrands: Set<CardType> = hashSetOf(CardType.BCMC)

    private val inputData: CardInputData = CardInputData()

    private val _outputDataFlow = MutableStateFlow(createOutputData())
    override val outputDataFlow: Flow<CardOutputData> = _outputDataFlow

    private val _componentStateFlow = MutableStateFlow(createComponentState())
    override val componentStateFlow: Flow<CardComponentState> = _componentStateFlow

    private val _exceptionFlow: MutableSharedFlow<CheckoutException> = MutableSingleEventSharedFlow()
    override val exceptionFlow: Flow<CheckoutException> = _exceptionFlow

    override val viewFlow: Flow<ComponentViewType?> = MutableStateFlow(CardComponentViewType)

    override val outputData: CardOutputData get() = _outputDataFlow.value

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
                    updateComponentState(outputData)
                },
                onFailure = { e ->
                    _exceptionFlow.tryEmit(ComponentException("Unable to fetch publicKey.", e))
                }
            )
        }
    }

    override fun updateInputData(update: CardInputData.() -> Unit) {
        inputData.update()
        onInputDataChanged()
    }

    private fun onInputDataChanged() {
        Logger.v(TAG, "onInputDataChanged")

        val outputData = createOutputData()
        _outputDataFlow.tryEmit(outputData)
        updateComponentState(outputData)
    }

    private fun createOutputData() = with(inputData) {
        CardOutputData(
            cardNumberState = FieldState(cardNumber, Validation.Valid),
            expiryDateState = FieldState(expiryDate, Validation.Valid),
            securityCodeState = validateSecurityCode(securityCode, storedDetectedCardTypes),
            holderNameState = FieldState(holderName, Validation.Valid),
            socialSecurityNumberState = FieldState(socialSecurityNumber, Validation.Valid),
            kcpBirthDateOrTaxNumberState = FieldState(kcpBirthDateOrTaxNumber, Validation.Valid),
            kcpCardPasswordState = FieldState(kcpCardPassword, Validation.Valid),
            addressState = AddressValidationUtils.makeValidEmptyAddressOutput(inputData.address),
            installmentState = FieldState(inputData.installmentOption, Validation.Valid),
            isStoredPaymentMethodEnable = isStorePaymentSelected,
            cvcUIState = makeCvcUIState(storedDetectedCardTypes?.cvcPolicy),
            expiryDateUIState = makeExpiryDateUIState(storedDetectedCardTypes?.expiryDatePolicy),
            holderNameUIState = InputFieldUIState.HIDDEN,
            showStorePaymentField = false,
            detectedCardTypes = listOfNotNull(storedDetectedCardTypes),
            isSocialSecurityNumberRequired = false,
            isKCPAuthRequired = false,
            addressUIState = AddressFormUIState.NONE,
            installmentOptions = emptyList(),
            countryOptions = emptyList(),
            stateOptions = emptyList(),
            cardBrands = emptyList(),
            isDualBranded = false,
            kcpBirthDateOrTaxNumberHint = null,
            componentMode = ComponentMode.STORED,
        )
    }

    @VisibleForTesting
    internal fun updateComponentState(outputData: CardOutputData) {
        Logger.v(TAG, "updateComponentState")
        val componentState = createComponentState(outputData)
        _componentStateFlow.tryEmit(componentState)
    }

    private fun createComponentState(
        outputData: CardOutputData = this.outputData
    ): CardComponentState {
        val cardNumber = outputData.cardNumberState.value

        val firstCardType = outputData.detectedCardTypes.firstOrNull()?.cardType

        val publicKey = publicKey

        // If data is not valid we just return empty object, encryption would fail and we don't pass unencrypted data.
        if (!outputData.isValid || publicKey == null) {
            return CardComponentState(
                paymentComponentData = PaymentComponentData(),
                isInputValid = outputData.isValid,
                isReady = publicKey != null,
                cardType = firstCardType,
                binValue = "",
                lastFourDigits = null
            )
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
            return CardComponentState(
                paymentComponentData = PaymentComponentData(),
                isInputValid = false,
                isReady = true,
                cardType = firstCardType,
                binValue = "",
                lastFourDigits = null
            )
        }

        return mapComponentState(
            encryptedCard,
            cardNumber,
            firstCardType,
        )
    }

    override fun getPaymentMethodType(): String {
        return storedPaymentMethod.type ?: PaymentMethodTypes.UNKNOWN
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

    private fun mapComponentState(
        encryptedCard: EncryptedCard,
        cardNumber: String,
        firstCardType: CardType?,
    ): CardComponentState {
        val cardPaymentMethod = CardPaymentMethod().apply {
            type = CardPaymentMethod.PAYMENT_METHOD_TYPE

            storedPaymentMethodId = getPaymentMethodId()

            if (!isCvcHidden()) {
                encryptedSecurityCode = encryptedCard.encryptedSecurityCode
            }

            try {
                // This call will throw an exception in case the merchant did not include our 3DS2 component/SDK
                // in their app. They can opt to use the standalone card component without 3DS2 or with another 3DS2
                // library.
                threeDS2SdkVersion = ThreeDS2Service.INSTANCE.sdkVersion
            } catch (e: ClassNotFoundException) {
                Logger.e(TAG, "threeDS2SdkVersion not set because 3DS2 SDK is not present in project.")
            } catch (e: NoClassDefFoundError) {
                Logger.e(TAG, "threeDS2SdkVersion not set because 3DS2 SDK is not present in project.")
            }
        }

        val paymentComponentData = makePaymentComponentData(cardPaymentMethod)

        val lastFour = cardNumber.takeLast(LAST_FOUR_LENGTH)

        return CardComponentState(
            paymentComponentData = paymentComponentData,
            isInputValid = true,
            isReady = true,
            cardType = firstCardType,
            binValue = "",
            lastFourDigits = lastFour
        )
    }

    private fun makePaymentComponentData(
        cardPaymentMethod: CardPaymentMethod
    ): PaymentComponentData<CardPaymentMethod> {
        return PaymentComponentData<CardPaymentMethod>().apply {
            paymentMethod = cardPaymentMethod
            shopperReference = configuration.shopperReference
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

        onInputDataChanged()
    }

    private fun makeCvcUIState(cvcPolicy: Brand.FieldPolicy?): InputFieldUIState {
        Logger.d(TAG, "makeCvcUIState: $cvcPolicy")
        return when {
            isCvcHidden() -> InputFieldUIState.HIDDEN
            // We treat CvcPolicy.HIDDEN as OPTIONAL for now to avoid hiding and showing the cvc field while the user
            // is typing the card number.
            cvcPolicy == Brand.FieldPolicy.OPTIONAL ||
                cvcPolicy == Brand.FieldPolicy.HIDDEN -> InputFieldUIState.OPTIONAL
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

    override fun getViewProvider(): ViewProvider = CardViewProvider

    override fun onCleared() {
        this.coroutineScope = null
    }

    companion object {
        private val TAG = LogUtil.getTag()
        private const val LAST_FOUR_LENGTH = 4
    }
}
