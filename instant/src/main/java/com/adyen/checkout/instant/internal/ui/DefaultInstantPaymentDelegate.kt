/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 4/11/2022.
 */

package com.adyen.checkout.instant.internal.ui

import androidx.lifecycle.LifecycleOwner
import com.adyen.checkout.components.core.Order
import com.adyen.checkout.components.core.PaymentComponentData
import com.adyen.checkout.components.core.PaymentMethod
import com.adyen.checkout.components.core.PaymentMethodTypes
import com.adyen.checkout.components.core.internal.PaymentComponentEvent
import com.adyen.checkout.components.core.internal.PaymentObserverRepository
import com.adyen.checkout.components.core.internal.data.api.AnalyticsRepository
import com.adyen.checkout.components.core.internal.ui.model.GenericComponentParams
import com.adyen.checkout.components.core.internal.util.bufferedChannel
import com.adyen.checkout.components.core.paymentmethod.GenericPaymentMethod
import com.adyen.checkout.components.core.paymentmethod.PaymentMethodDetails
import com.adyen.checkout.core.internal.util.LogUtil
import com.adyen.checkout.core.internal.util.Logger
import com.adyen.checkout.instant.InstantComponentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class DefaultInstantPaymentDelegate(
    private val observerRepository: PaymentObserverRepository,
    private val paymentMethod: PaymentMethod,
    private val order: Order?,
    override val componentParams: GenericComponentParams,
    private val analyticsRepository: AnalyticsRepository,
) : InstantPaymentDelegate {

    override val componentStateFlow: StateFlow<InstantComponentState> = MutableStateFlow(createComponentState())

    private val submitChannel: Channel<InstantComponentState> = bufferedChannel()
    override val submitFlow: Flow<InstantComponentState> = submitChannel.receiveAsFlow()

    init {
        submitChannel.trySend(componentStateFlow.value)
    }

    override fun getPaymentMethodType(): String = paymentMethod.type ?: PaymentMethodTypes.UNKNOWN

    private fun createComponentState(): InstantComponentState {
        val paymentComponentData = PaymentComponentData<PaymentMethodDetails>(
            paymentMethod = GenericPaymentMethod(
                type = paymentMethod.type,
                checkoutAttemptId = analyticsRepository.getCheckoutAttemptId(),
            ),
            order = order,
            amount = componentParams.amount,
        )
        return InstantComponentState(paymentComponentData, isInputValid = true, isReady = true)
    }

    override fun initialize(coroutineScope: CoroutineScope) {
        setupAnalytics(coroutineScope)
    }

    private fun setupAnalytics(coroutineScope: CoroutineScope) {
        Logger.v(TAG, "setupAnalytics")
        coroutineScope.launch {
            analyticsRepository.setupAnalytics()
        }
    }

    override fun observe(
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        callback: (PaymentComponentEvent<InstantComponentState>) -> Unit
    ) {
        observerRepository.addObservers(
            stateFlow = componentStateFlow,
            exceptionFlow = null,
            submitFlow = submitFlow,
            lifecycleOwner = lifecycleOwner,
            coroutineScope = coroutineScope,
            callback = callback
        )
    }

    override fun removeObserver() {
        observerRepository.removeObservers()
    }

    override fun onCleared() {
        removeObserver()
    }

    companion object {
        private val TAG = LogUtil.getTag()
    }
}
