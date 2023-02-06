/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 6/2/2023.
 */

package com.adyen.checkout.sessions

import android.os.Parcel
import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.PaymentComponentEvent
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.base.BaseComponentCallback
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.sessions.interactor.SessionCallResult
import com.adyen.checkout.sessions.interactor.SessionInteractor
import com.adyen.checkout.sessions.model.SessionModel
import com.adyen.checkout.sessions.model.SessionPaymentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
internal class SessionComponentEventHandlerTest(
    @Mock private val sessionInteractor: SessionInteractor,
    @Mock private val sessionSavedStateHandleContainer: SessionSavedStateHandleContainer,
) {

    private lateinit var sessionComponentEventHandler: SessionComponentEventHandler<PaymentComponentState<*>>

    @BeforeEach
    fun beforeEach() {
        sessionComponentEventHandler = SessionComponentEventHandler(sessionInteractor, sessionSavedStateHandleContainer)
        Logger.setLogcatLevel(Logger.NONE)
    }

    @Test
    fun `when session is changed, then session data should be updated`() {
        val sessionFlow = MutableStateFlow(SessionModel("id", "sessionData"))
        whenever(sessionInteractor.sessionFlow) doReturn sessionFlow
        sessionComponentEventHandler.initialize(CoroutineScope(UnconfinedTestDispatcher()))

        sessionFlow.tryEmit(SessionModel("id", "updatedSessionData"))

        verify(sessionSavedStateHandleContainer).updateSessionData("updatedSessionData")
    }

    @Nested
    @DisplayName("when payment component event")
    inner class OnPaymentComponentEventTest {

        @BeforeEach
        fun beforeEach() {
            whenever(sessionInteractor.sessionFlow) doReturn flowOf()
            sessionComponentEventHandler.initialize(CoroutineScope(UnconfinedTestDispatcher()))
        }

        @Test
        fun `and component callback is wrongly typed, then an error should be thrown`() {
            assertThrows<CheckoutException> {
                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Submit(createPaymentComponentState()),
                    object : BaseComponentCallback {}
                )
            }
        }

        @Nested
        @DisplayName("is Submit")
        inner class SubmitTest {

            @Test
            fun `then loading state should be propagated properly`() {
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Submit(createPaymentComponentState()),
                    callback
                )

                callback.assertLoadingStatesEqual(listOf(true, false))
            }

            @Test
            fun `and result is Action, then action should be propagated`() = runTest {
                val action = createTestAction()
                whenever(sessionInteractor.onPaymentsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Payments.Action(action)
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Submit(createPaymentComponentState()),
                    callback
                )

                callback.assertOnActionEquals(action)
            }

            @Test
            fun `and result is Error, then error should be propagated`() = runTest {
                val error = RuntimeException("Test")
                whenever(sessionInteractor.onPaymentsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Payments.Error(error)
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Submit(createPaymentComponentState()),
                    callback
                )

                callback.assertOnErrorCauseEquals(error)
            }

            @Test
            fun `and result is Finished, then result should be propagated`() = runTest {
                val result = createSessionPaymentResult()
                whenever(sessionInteractor.onPaymentsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Payments.Finished(result)
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Submit(createPaymentComponentState()),
                    callback
                )

                callback.assertOnFinishedEquals(result)
            }

            @Test
            fun `and result is NotFullyPaidOrder, then result should be propagated`() = runTest {
                val result = createSessionPaymentResult()
                whenever(sessionInteractor.onPaymentsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Payments.NotFullyPaidOrder(result)
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Submit(createPaymentComponentState()),
                    callback
                )

                callback.assertOnFinishedEquals(result)
            }

            @Test
            fun `and result is RefusedPartialPayment, then result should be propagated`() = runTest {
                val result = createSessionPaymentResult()
                whenever(sessionInteractor.onPaymentsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Payments.RefusedPartialPayment(result)
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Submit(createPaymentComponentState()),
                    callback
                )

                callback.assertOnFinishedEquals(result)
            }

            @Test
            fun `and result is TakenOver, then this should be set`() = runTest {
                whenever(sessionInteractor.onPaymentsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Payments.TakenOver
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Submit(createPaymentComponentState()),
                    callback
                )

                verify(sessionSavedStateHandleContainer).isFlowTakenOver = true
            }
        }

        @Nested
        @DisplayName("is ActionDetails")
        inner class ActionDetailsTest {

            @Test
            fun `then loading state should be propagated properly`() {
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.ActionDetails(ActionComponentData()),
                    callback
                )

                callback.assertLoadingStatesEqual(listOf(true, false))
            }

            @Test
            fun `and result is Action, then action should be propagated`() = runTest {
                val action = createTestAction()
                whenever(sessionInteractor.onDetailsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Details.Action(action)
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.ActionDetails(ActionComponentData()),
                    callback
                )

                callback.assertOnActionEquals(action)
            }

            @Test
            fun `and result is Error, then error should be propagated`() = runTest {
                val error = RuntimeException("Test")
                whenever(sessionInteractor.onDetailsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Details.Error(error)
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.ActionDetails(ActionComponentData()),
                    callback
                )

                callback.assertOnErrorCauseEquals(error)
            }

            @Test
            fun `and result is Finished, then result should be propagated`() = runTest {
                val result = createSessionPaymentResult()
                whenever(sessionInteractor.onDetailsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Details.Finished(result)
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.ActionDetails(ActionComponentData()),
                    callback
                )

                callback.assertOnFinishedEquals(result)
            }

            @Test
            fun `and result is TakenOver, then this should be set`() = runTest {
                whenever(sessionInteractor.onDetailsCallRequested(any(), any(), any())) doReturn
                    SessionCallResult.Details.TakenOver
                val callback = TestSessionComponentCallback()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.ActionDetails(ActionComponentData()),
                    callback
                )

                verify(sessionSavedStateHandleContainer).isFlowTakenOver = true
            }
        }

        @Nested
        @DisplayName("is StateChanged")
        inner class StateChangedTest {

            @Test
            fun `then state change should be propagated`() = runTest {
                val callback = TestSessionComponentCallback()
                val componentState = createPaymentComponentState()

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.StateChanged(componentState),
                    callback
                )

                callback.assertOnStateChangedEquals(componentState)
            }
        }

        @Nested
        @DisplayName("is Error")
        inner class ErrorTest {

            @Test
            fun `then error be propagated`() = runTest {
                val callback = TestSessionComponentCallback()
                val error = ComponentError(CheckoutException("Test"))

                sessionComponentEventHandler.onPaymentComponentEvent(
                    PaymentComponentEvent.Error(error),
                    callback
                )

                callback.assertOnErrorEquals(error)
            }
        }
    }

    private fun createPaymentComponentState() = PaymentComponentState(
        data = PaymentComponentData(),
        isInputValid = false,
        isReady = false,
    )

    private fun createTestAction(
        type: String = "test",
        paymentData: String = "paymentData",
        paymentMethodType: String = "paymentMethodType",
    ) = object : Action() {
        override var type: String? = type
        override var paymentData: String? = paymentData
        override var paymentMethodType: String? = paymentMethodType
        override fun writeToParcel(dest: Parcel, flags: Int) = Unit
    }

    private fun createSessionPaymentResult() = SessionPaymentResult(
        sessionResult = "sessionResult",
        sessionData = "sessionData",
        resultCode = "resultCode",
        order = null,
    )

    private class TestSessionComponentCallback : SessionComponentCallback<PaymentComponentState<*>> {

        private var loadingStates = mutableListOf<Boolean>()

        private var onActionValue: Action? = null

        private var onErrorValue: ComponentError? = null

        private var onFinishedValue: SessionPaymentResult? = null

        private var onStateChangedValue: PaymentComponentState<*>? = null

        override fun onAction(action: Action) {
            onActionValue = action
        }

        override fun onError(componentError: ComponentError) {
            onErrorValue = componentError
        }

        override fun onFinished(result: SessionPaymentResult) {
            onFinishedValue = result
        }

        override fun onLoading(isLoading: Boolean) {
            loadingStates.add(isLoading)
        }

        override fun onStateChanged(state: PaymentComponentState<*>) {
            onStateChangedValue = state
        }

        fun assertLoadingStatesEqual(expected: List<Boolean>) {
            assertEquals(expected, loadingStates)
        }

        fun assertOnActionEquals(expected: Action?) {
            assertEquals(expected, onActionValue)
        }

        fun assertOnErrorCauseEquals(expected: Throwable?) {
            assertEquals(expected, onErrorValue?.exception?.cause)
        }

        fun assertOnErrorEquals(expected: ComponentError?) {
            assertEquals(expected, onErrorValue)
        }

        fun assertOnFinishedEquals(expected: SessionPaymentResult?) {
            assertEquals(expected, onFinishedValue)
        }

        fun assertOnStateChangedEquals(expected: PaymentComponentState<*>?) {
            assertEquals(expected, onStateChangedValue)
        }
    }
}
