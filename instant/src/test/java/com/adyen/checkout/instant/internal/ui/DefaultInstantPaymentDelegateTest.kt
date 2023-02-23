/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 9/11/2022.
 */

package com.adyen.checkout.instant.internal.ui

import app.cash.turbine.test
import com.adyen.checkout.components.core.internal.data.api.AnalyticsRepository
import com.adyen.checkout.components.core.internal.ui.model.GenericComponentParamsMapper
import com.adyen.checkout.components.core.PaymentMethod
import com.adyen.checkout.components.core.OrderRequest
import com.adyen.checkout.components.core.internal.PaymentObserverRepository
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.instant.InstantPaymentConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class DefaultInstantPaymentDelegateTest(
    @Mock private val analyticsRepository: AnalyticsRepository,
) {

    private lateinit var delegate: DefaultInstantPaymentDelegate

    @BeforeEach
    fun before() {
        val configuration = InstantPaymentConfiguration.Builder(
            Locale.US,
            Environment.TEST,
            TEST_CLIENT_KEY
        ).build()
        delegate = DefaultInstantPaymentDelegate(
            observerRepository = PaymentObserverRepository(),
            paymentMethod = PaymentMethod(type = TYPE),
            order = TEST_ORDER,
            componentParams = GenericComponentParamsMapper().mapToParams(configuration),
            analyticsRepository = analyticsRepository
        )
        Logger.setLogcatLevel(Logger.NONE)
    }

    @Test
    fun `when subscribed then component state flow should propagate a valid state`() = runTest {
        delegate.componentStateFlow.test {
            with(awaitItem()) {
                assertEquals(TYPE, data.paymentMethod?.type)
                assertEquals(TEST_ORDER, data.order)
                assertTrue(isInputValid)
                assertTrue(isValid)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when delegate is initialized then analytics event is sent`() = runTest {
        delegate.initialize(CoroutineScope(UnconfinedTestDispatcher()))
        verify(analyticsRepository).sendAnalyticsEvent()
    }

    companion object {
        private const val TEST_CLIENT_KEY = "test_qwertyuiopasdfghjklzxcvbnmqwerty"
        private const val TYPE = "txVariant"
        private val TEST_ORDER = OrderRequest("PSP", "ORDER_DATA")
    }
}