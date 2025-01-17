/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 17/11/2022.
 */

package com.adyen.checkout.bcmc.internal.ui.model

import com.adyen.checkout.bcmc.BcmcConfiguration
import com.adyen.checkout.components.core.Amount
import com.adyen.checkout.components.core.internal.ui.model.AnalyticsParams
import com.adyen.checkout.components.core.internal.ui.model.AnalyticsParamsLevel
import com.adyen.checkout.components.core.internal.ui.model.GenericComponentParams
import com.adyen.checkout.components.core.internal.ui.model.SessionParams
import com.adyen.checkout.core.Environment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.Locale

internal class BcmcComponentParamsMapperTest {

    @Test
    fun `when parent configuration is null and custom bcmc configuration fields are null then all fields should match`() {
        val bcmcConfiguration = getBcmcConfigurationBuilder()
            .build()

        val params = BcmcComponentParamsMapper(null, null).mapToParams(bcmcConfiguration, null)

        val expected = getBcmcComponentParams()

        assertEquals(expected, params)
    }

    @Test
    fun `when parent configuration is null and custom bcmc configuration fields are set then all fields should match`() {
        val shopperReference = "SHOPPER_REFERENCE_1"

        val bcmcConfiguration = getBcmcConfigurationBuilder()
            .setShopperReference(shopperReference)
            .setHolderNameRequired(true)
            .setShowStorePaymentField(true)
            .setSubmitButtonVisible(false)
            .build()

        val params = BcmcComponentParamsMapper(null, null).mapToParams(bcmcConfiguration, null)

        val expected = getBcmcComponentParams(
            isHolderNameRequired = true,
            shopperReference = shopperReference,
            isStorePaymentFieldVisible = true,
            isSubmitButtonVisible = false
        )

        assertEquals(expected, params)
    }

    @Test
    fun `when parent configuration is set then parent configuration fields should override bcmc configuration fields`() {
        val bcmcConfiguration = getBcmcConfigurationBuilder()
            .build()

        // this is in practice DropInComponentParams, but we don't have access to it in this module and any
        // ComponentParams class can work
        val overrideParams = GenericComponentParams(
            shopperLocale = Locale.GERMAN,
            environment = Environment.EUROPE,
            clientKey = TEST_CLIENT_KEY_2,
            analyticsParams = AnalyticsParams(AnalyticsParamsLevel.NONE),
            isCreatedByDropIn = true,
            amount = Amount(
                currency = "USD",
                value = 25_00L
            )
        )

        val params = BcmcComponentParamsMapper(overrideParams, null).mapToParams(bcmcConfiguration, null)

        val expected = getBcmcComponentParams(
            shopperLocale = Locale.GERMAN,
            environment = Environment.EUROPE,
            clientKey = TEST_CLIENT_KEY_2,
            analyticsParams = AnalyticsParams(AnalyticsParamsLevel.NONE),
            isCreatedByDropIn = true,
            amount = Amount(
                currency = "USD",
                value = 25_00L
            ),
        )

        assertEquals(expected, params)
    }

    @ParameterizedTest
    @MethodSource("enableStoreDetailsSource")
    @Suppress("MaxLineLength")
    fun `isStorePaymentFieldVisible should match value set in sessions if it exists, otherwise should match configuration`(
        configurationValue: Boolean,
        sessionsValue: Boolean?,
        expectedValue: Boolean
    ) {
        val bcmcConfiguration = getBcmcConfigurationBuilder()
            .setShowStorePaymentField(configurationValue)
            .build()

        val params = BcmcComponentParamsMapper(null, null).mapToParams(
            bcmcConfiguration = bcmcConfiguration,
            sessionParams = SessionParams(
                enableStoreDetails = sessionsValue,
                installmentOptions = null,
                amount = null,
                returnUrl = "",
            )
        )

        val expected = getBcmcComponentParams(isStorePaymentFieldVisible = expectedValue)

        assertEquals(expected, params)
    }

    @ParameterizedTest
    @MethodSource("amountSource")
    fun `amount should match value set in sessions if it exists, then should match drop in value, then configuration`(
        configurationValue: Amount,
        dropInValue: Amount?,
        sessionsValue: Amount?,
        expectedValue: Amount
    ) {
        val bcmcConfiguration = getBcmcConfigurationBuilder()
            .setAmount(configurationValue)
            .build()

        // this is in practice DropInComponentParams, but we don't have access to it in this module and any
        // ComponentParams class can work
        val overrideParams = dropInValue?.let { getBcmcComponentParams(amount = it) }

        val params = BcmcComponentParamsMapper(overrideParams, null).mapToParams(
            bcmcConfiguration,
            sessionParams = SessionParams(
                enableStoreDetails = null,
                installmentOptions = null,
                amount = sessionsValue,
                returnUrl = "",
            )
        )

        val expected = getBcmcComponentParams(
            amount = expectedValue
        )

        assertEquals(expected, params)
    }

    private fun getBcmcConfigurationBuilder() = BcmcConfiguration.Builder(
        shopperLocale = Locale.US,
        environment = Environment.TEST,
        clientKey = TEST_CLIENT_KEY_1
    )

    @Suppress("LongParameterList")
    private fun getBcmcComponentParams(
        shopperLocale: Locale = Locale.US,
        environment: Environment = Environment.TEST,
        clientKey: String = TEST_CLIENT_KEY_1,
        analyticsParams: AnalyticsParams = AnalyticsParams(AnalyticsParamsLevel.ALL),
        isCreatedByDropIn: Boolean = false,
        amount: Amount? = null,
        isSubmitButtonVisible: Boolean = true,
        isHolderNameRequired: Boolean = false,
        shopperReference: String? = null,
        isStorePaymentFieldVisible: Boolean = false,
    ) = BcmcComponentParams(
        shopperLocale = shopperLocale,
        environment = environment,
        clientKey = clientKey,
        analyticsParams = analyticsParams,
        isCreatedByDropIn = isCreatedByDropIn,
        amount = amount,
        isSubmitButtonVisible = isSubmitButtonVisible,
        isHolderNameRequired = isHolderNameRequired,
        shopperReference = shopperReference,
        isStorePaymentFieldVisible = isStorePaymentFieldVisible
    )

    companion object {
        private const val TEST_CLIENT_KEY_1 = "test_qwertyuiopasdfghjklzxcvbnmqwerty"
        private const val TEST_CLIENT_KEY_2 = "live_qwertyui34566776787zxcvbnmqwerty"

        @JvmStatic
        fun enableStoreDetailsSource() = listOf(
            // configurationValue, sessionsValue, expectedValue
            arguments(false, false, false),
            arguments(false, true, true),
            arguments(true, false, false),
            arguments(true, true, true),
            arguments(false, null, false),
            arguments(true, null, true),
        )

        @JvmStatic
        fun amountSource() = listOf(
            // configurationValue, dropInValue, sessionsValue, expectedValue
            arguments(Amount("EUR", 100), Amount("USD", 200), Amount("CAD", 300), Amount("CAD", 300)),
            arguments(Amount("EUR", 100), Amount("USD", 200), null, Amount("USD", 200)),
            arguments(Amount("EUR", 100), null, null, Amount("EUR", 100)),
        )
    }
}
