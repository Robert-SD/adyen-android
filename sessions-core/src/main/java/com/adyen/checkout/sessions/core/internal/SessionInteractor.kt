/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 4/1/2023.
 */

package com.adyen.checkout.sessions.core.internal

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.adyen.checkout.components.core.ActionComponentData
import com.adyen.checkout.components.core.BalanceResult
import com.adyen.checkout.components.core.OrderRequest
import com.adyen.checkout.components.core.OrderResponse
import com.adyen.checkout.components.core.PaymentComponentState
import com.adyen.checkout.components.core.StatusResponseUtils
import com.adyen.checkout.components.core.paymentmethod.PaymentMethodDetails
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.exception.MethodNotImplementedException
import com.adyen.checkout.core.internal.util.LogUtil
import com.adyen.checkout.sessions.core.SessionModel
import com.adyen.checkout.sessions.core.SessionPaymentResult
import com.adyen.checkout.sessions.core.internal.data.api.SessionRepository
import com.adyen.checkout.sessions.core.internal.data.model.SessionDetailsResponse
import com.adyen.checkout.sessions.core.internal.data.model.SessionPaymentsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Suppress("TooManyFunctions")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SessionInteractor(
    private val sessionRepository: SessionRepository,
    sessionModel: SessionModel,
    isFlowTakenOver: Boolean,
) {

    @VisibleForTesting
    internal var isFlowTakenOver: Boolean = isFlowTakenOver
        private set

    private val _sessionFlow = MutableStateFlow(sessionModel)
    val sessionFlow: Flow<SessionModel> = _sessionFlow

    private val sessionModel: SessionModel get() = _sessionFlow.value

    suspend fun <T : PaymentComponentState<*>> onPaymentsCallRequested(
        paymentComponentState: T,
        merchantCall: (T) -> Boolean,
        merchantCallName: String,
    ): SessionCallResult.Payments {
        return checkIfCallWasHandled(
            merchantCall = { merchantCall(paymentComponentState) },
            internalCall = { makePaymentsCallInternal(paymentComponentState) },
            merchantMethodName = merchantCallName,
            takenOverFactory = { SessionCallResult.Payments.TakenOver }
        )
    }

    private suspend fun makePaymentsCallInternal(
        paymentComponentState: PaymentComponentState<*>
    ): SessionCallResult.Payments {
        sessionRepository.submitPayment(sessionModel, paymentComponentState.data)
            .fold(
                onSuccess = { response ->
                    updateSessionData(response.sessionData)

                    val action = response.action
                    return when {
                        response.isRefusedInPartialPaymentFlow() ->
                            SessionCallResult.Payments.RefusedPartialPayment(response.mapToSessionPaymentResult())
                        action != null -> SessionCallResult.Payments.Action(action)
                        response.order.isNonFullyPaid() ->
                            SessionCallResult.Payments.NotFullyPaidOrder(response.mapToSessionPaymentResult())
                        else -> SessionCallResult.Payments.Finished(response.mapToSessionPaymentResult())
                    }
                },
                onFailure = {
                    return SessionCallResult.Payments.Error(throwable = it)
                }
            )
    }

    private fun SessionPaymentsResponse.isRefusedInPartialPaymentFlow() = isRefused() && order.isNonFullyPaid()

    private fun SessionPaymentsResponse.isRefused() =
        resultCode.equals(other = StatusResponseUtils.RESULT_REFUSED, ignoreCase = true)

    private fun OrderResponse?.isNonFullyPaid() = (this?.remainingAmount?.value ?: 0) > 0

    suspend fun onDetailsCallRequested(
        actionComponentData: ActionComponentData,
        merchantCall: (ActionComponentData) -> Boolean,
        merchantCallName: String,
    ): SessionCallResult.Details {
        return checkIfCallWasHandled(
            merchantCall = { merchantCall(actionComponentData) },
            internalCall = { makeDetailsCallInternal(actionComponentData) },
            merchantMethodName = merchantCallName,
            takenOverFactory = { SessionCallResult.Details.TakenOver }
        )
    }

    private suspend fun makeDetailsCallInternal(actionComponentData: ActionComponentData): SessionCallResult.Details {
        sessionRepository.submitDetails(sessionModel, actionComponentData)
            .fold(
                onSuccess = { response ->
                    updateSessionData(response.sessionData)

                    return when (val action = response.action) {
                        null -> SessionCallResult.Details.Finished(response.mapToSessionPaymentResult())
                        else -> SessionCallResult.Details.Action(action)
                    }
                },
                onFailure = {
                    return SessionCallResult.Details.Error(throwable = it)
                }
            )
    }

    suspend fun checkBalance(
        paymentMethodData: PaymentMethodDetails,
        merchantCall: (PaymentMethodDetails) -> Boolean,
        merchantCallName: String,
    ): SessionCallResult.Balance {
        return checkIfCallWasHandled(
            merchantCall = { merchantCall(paymentMethodData) },
            internalCall = { makeCheckBalanceCallInternal(paymentMethodData) },
            merchantMethodName = merchantCallName,
            takenOverFactory = { SessionCallResult.Balance.TakenOver },
        )
    }

    private suspend fun makeCheckBalanceCallInternal(
        paymentMethodData: PaymentMethodDetails
    ): SessionCallResult.Balance {
        sessionRepository.checkBalance(sessionModel, paymentMethodData)
            .fold(
                onSuccess = { response ->
                    updateSessionData(response.sessionData)
                    return if (response.balance.value <= 0) {
                        SessionCallResult.Balance.Error(CheckoutException("Not enough balance"))
                    } else {
                        val balanceResult = BalanceResult(response.balance, response.transactionLimit)
                        SessionCallResult.Balance.Successful(balanceResult)
                    }
                },
                onFailure = {
                    return SessionCallResult.Balance.Error(throwable = it)
                }
            )
    }

    suspend fun createOrder(
        merchantCall: () -> Boolean,
        merchantCallName: String,
    ): SessionCallResult.CreateOrder {
        return checkIfCallWasHandled(
            merchantCall = { merchantCall() },
            internalCall = { makeCreateOrderInternal() },
            merchantMethodName = merchantCallName,
            takenOverFactory = { SessionCallResult.CreateOrder.TakenOver }
        )
    }

    private suspend fun makeCreateOrderInternal(): SessionCallResult.CreateOrder {
        sessionRepository.createOrder(sessionModel)
            .fold(
                onSuccess = { response ->
                    updateSessionData(response.sessionData)

                    val order = OrderResponse(
                        pspReference = response.pspReference,
                        orderData = response.orderData,
                        amount = null,
                        remainingAmount = null,
                    )
                    return SessionCallResult.CreateOrder.Successful(order)
                },
                onFailure = {
                    return SessionCallResult.CreateOrder.Error(throwable = it)
                }
            )
    }

    suspend fun cancelOrder(
        order: OrderRequest,
        merchantCall: (OrderRequest) -> Boolean,
        merchantCallName: String,
    ): SessionCallResult.CancelOrder {
        return checkIfCallWasHandled(
            merchantCall = { merchantCall(order) },
            internalCall = { makeCancelOrderCallInternal(order) },
            merchantMethodName = merchantCallName,
            takenOverFactory = { SessionCallResult.CancelOrder.TakenOver }
        )
    }

    private suspend fun makeCancelOrderCallInternal(
        order: OrderRequest,
    ): SessionCallResult.CancelOrder {
        sessionRepository.cancelOrder(sessionModel, order)
            .fold(
                onSuccess = {
                    updateSessionData(it.sessionData)

                    return SessionCallResult.CancelOrder.Successful
                },
                onFailure = {
                    return SessionCallResult.CancelOrder.Error(throwable = it)
                }
            )
    }

    suspend fun updatePaymentMethods(order: OrderResponse? = null): SessionCallResult.UpdatePaymentMethods {
        val orderRequest = order?.let {
            OrderRequest(
                pspReference = order.pspReference,
                orderData = order.orderData
            )
        }

        sessionRepository.setupSession(sessionModel, orderRequest)
            .fold(
                onSuccess = { response ->
                    updateSessionData(response.sessionData)

                    val paymentMethods = response.paymentMethods
                    return if (paymentMethods != null) {
                        SessionCallResult.UpdatePaymentMethods.Successful(paymentMethods, order)
                    } else {
                        SessionCallResult.UpdatePaymentMethods.Error(
                            throwable = CheckoutException(
                                errorMessage = "Payment methods should not be null"
                            )
                        )
                    }
                },
                onFailure = {
                    return SessionCallResult.UpdatePaymentMethods.Error(throwable = it)
                }
            )
    }

    private suspend fun <T : SessionCallResult> checkIfCallWasHandled(
        merchantCall: suspend () -> Boolean,
        internalCall: suspend () -> T,
        merchantMethodName: String,
        takenOverFactory: () -> T
    ): T {
        val callWasHandled = merchantCall()
        return if (!callWasHandled) {
            if (isFlowTakenOver) {
                throw MethodNotImplementedException(
                    "Sessions flow was already taken over in a previous call, $merchantMethodName should be implemented"
                )
            } else {
                internalCall()
            }
        } else {
            if (!isFlowTakenOver) {
                isFlowTakenOver = true
            }
            takenOverFactory()
        }
    }

    private fun updateSessionData(sessionData: String) {
        _sessionFlow.update { it.copy(sessionData = sessionData) }
    }

    private fun SessionPaymentsResponse.mapToSessionPaymentResult() = SessionPaymentResult(
        sessionResult = sessionResult,
        sessionData = sessionData,
        resultCode = resultCode,
        order = order,
    )

    private fun SessionDetailsResponse.mapToSessionPaymentResult() = SessionPaymentResult(
        sessionResult = sessionResult,
        sessionData = sessionData,
        resultCode = resultCode,
        order = order,
    )

    companion object {
        private val TAG = LogUtil.getTag()
    }
}