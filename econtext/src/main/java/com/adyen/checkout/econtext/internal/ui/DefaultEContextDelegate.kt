/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 13/1/2023.
 */

package com.adyen.checkout.econtext.internal.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import com.adyen.checkout.components.PaymentComponentEvent
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.analytics.AnalyticsRepository
import com.adyen.checkout.components.base.ButtonComponentParams
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.payments.request.EContextPaymentMethod
import com.adyen.checkout.components.model.payments.request.Order
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.repository.PaymentObserverRepository
import com.adyen.checkout.components.ui.FieldState
import com.adyen.checkout.components.ui.PaymentComponentUIEvent
import com.adyen.checkout.components.ui.PaymentComponentUIState
import com.adyen.checkout.components.ui.SubmitHandler
import com.adyen.checkout.components.ui.Validation
import com.adyen.checkout.components.ui.view.ButtonComponentViewType
import com.adyen.checkout.components.ui.view.ComponentViewType
import com.adyen.checkout.components.util.PaymentMethodTypes
import com.adyen.checkout.components.util.ValidationUtils
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.econtext.R
import com.adyen.checkout.econtext.internal.ui.model.EContextInputData
import com.adyen.checkout.econtext.internal.ui.model.EContextOutputData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "LongParameterList")
internal class DefaultEContextDelegate<EContextPaymentMethodT : EContextPaymentMethod>(
    private val observerRepository: PaymentObserverRepository,
    override val componentParams: ButtonComponentParams,
    private val paymentMethod: PaymentMethod,
    private val order: Order?,
    private val analyticsRepository: AnalyticsRepository,
    private val submitHandler: SubmitHandler<PaymentComponentState<EContextPaymentMethodT>>,
    private val typedPaymentMethodFactory: () -> EContextPaymentMethodT,
) : EContextDelegate<EContextPaymentMethodT> {

    private val inputData: EContextInputData = EContextInputData()

    private val _outputDataFlow = MutableStateFlow(createOutputData())
    override val outputDataFlow: Flow<EContextOutputData> = _outputDataFlow

    override val outputData: EContextOutputData get() = _outputDataFlow.value

    private val _componentStateFlow = MutableStateFlow(createComponentState())
    override val componentStateFlow: Flow<PaymentComponentState<EContextPaymentMethodT>> = _componentStateFlow

    private val _viewFlow: MutableStateFlow<ComponentViewType?> = MutableStateFlow(EContextComponentViewType)
    override val viewFlow: Flow<ComponentViewType?> = _viewFlow

    override val submitFlow: Flow<PaymentComponentState<EContextPaymentMethodT>> = submitHandler.submitFlow
    override val uiStateFlow: Flow<PaymentComponentUIState> = submitHandler.uiStateFlow
    override val uiEventFlow: Flow<PaymentComponentUIEvent> = submitHandler.uiEventFlow

    override fun initialize(coroutineScope: CoroutineScope) {
        submitHandler.initialize(coroutineScope, componentStateFlow)
        sendAnalyticsEvent(coroutineScope)
    }

    private fun sendAnalyticsEvent(coroutineScope: CoroutineScope) {
        Logger.v(TAG, "sendAnalyticsEvent")
        coroutineScope.launch {
            analyticsRepository.sendAnalyticsEvent()
        }
    }

    override fun updateInputData(update: EContextInputData.() -> Unit) {
        inputData.update()
        onInputDataChanged()
    }

    private fun onInputDataChanged() {
        val outputData = createOutputData()
        _outputDataFlow.tryEmit(outputData)
        updateComponentState(outputData)
    }

    private fun createOutputData(): EContextOutputData {
        return EContextOutputData(
            firstNameState = validateFirstName(inputData.firstName),
            lastNameState = validateLastName(inputData.lastName),
            phoneNumberState = validatePhoneNumber(inputData.mobileNumber, inputData.countryCode),
            emailAddressState = validateEmailAddress(inputData.emailAddress)
        )
    }

    @VisibleForTesting
    internal fun updateComponentState(outputData: EContextOutputData) {
        val componentState = createComponentState(outputData)
        _componentStateFlow.tryEmit(componentState)
    }

    private fun createComponentState(
        outputData: EContextOutputData = this.outputData
    ): PaymentComponentState<EContextPaymentMethodT> {
        val eContextPaymentMethod = typedPaymentMethodFactory().apply {
            type = getPaymentMethodType()
            firstName = outputData.firstNameState.value
            lastName = outputData.lastNameState.value
            telephoneNumber = outputData.phoneNumberState.value
            shopperEmail = outputData.emailAddressState.value
        }
        val isInputValid = outputData.isValid
        val paymentComponentData = PaymentComponentData(
            paymentMethod = eContextPaymentMethod,
            order = this@DefaultEContextDelegate.order,
        )
        return PaymentComponentState(paymentComponentData, isInputValid, true)
    }

    private fun validateFirstName(firstName: String): FieldState<String> {
        val validation = if (firstName.isNotBlank()) {
            Validation.Valid
        } else {
            Validation.Invalid(R.string.checkout_econtext_first_name_invalid)
        }
        return FieldState(firstName, validation)
    }

    private fun validateLastName(lastName: String): FieldState<String> {
        val validation = if (lastName.isNotBlank()) {
            Validation.Valid
        } else {
            Validation.Invalid(R.string.checkout_econtext_last_name_invalid)
        }
        return FieldState(lastName, validation)
    }

    private fun validatePhoneNumber(phoneNumber: String, countryCode: String): FieldState<String> {
        val sanitizedNumber = phoneNumber.trimStart('0')
        val fullPhoneNumber = countryCode + sanitizedNumber
        val validation = if (fullPhoneNumber.isNotEmpty() && ValidationUtils.isPhoneNumberValid(fullPhoneNumber)) {
            Validation.Valid
        } else {
            Validation.Invalid(R.string.checkout_econtext_phone_number_invalid)
        }
        return FieldState(fullPhoneNumber, validation)
    }

    private fun validateEmailAddress(emailAddress: String): FieldState<String> {
        val validation = if (emailAddress.isNotEmpty() && ValidationUtils.isEmailValid(emailAddress)) {
            Validation.Valid
        } else {
            Validation.Invalid(R.string.checkout_econtext_shopper_email_invalid)
        }
        return FieldState(emailAddress, validation)
    }

    override fun getPaymentMethodType(): String {
        return paymentMethod.type ?: PaymentMethodTypes.UNKNOWN
    }

    override fun observe(
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        callback: (PaymentComponentEvent<PaymentComponentState<EContextPaymentMethodT>>) -> Unit
    ) {
        observerRepository.addObservers(
            stateFlow = componentStateFlow,
            exceptionFlow = null,
            submitFlow = submitFlow,
            lifecycleOwner = lifecycleOwner,
            coroutineScope = coroutineScope,
            callback = callback,
        )
    }

    override fun removeObserver() {
        observerRepository.removeObservers()
    }

    override fun onCleared() {
        removeObserver()
    }

    override fun onSubmit() {
        val state = _componentStateFlow.value
        submitHandler.onSubmit(state)
    }

    override fun isConfirmationRequired(): Boolean {
        return _viewFlow.value is ButtonComponentViewType
    }

    override fun shouldShowSubmitButton(): Boolean {
        return isConfirmationRequired() && componentParams.isSubmitButtonVisible
    }

    override fun setInteractionBlocked(isInteractionBlocked: Boolean) {
        submitHandler.setInteractionBlocked(isInteractionBlocked)
    }

    companion object {
        private val TAG = LogUtil.getTag()
    }
}