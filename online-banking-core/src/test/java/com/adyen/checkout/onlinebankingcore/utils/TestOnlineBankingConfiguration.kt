/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 21/11/2022.
 */

package com.adyen.checkout.onlinebankingcore.utils

import android.content.Context
import com.adyen.checkout.components.core.Amount
import com.adyen.checkout.components.core.AnalyticsConfiguration
import com.adyen.checkout.components.core.internal.BaseConfigurationBuilder
import com.adyen.checkout.components.core.internal.ButtonConfiguration
import com.adyen.checkout.components.core.internal.ButtonConfigurationBuilder
import com.adyen.checkout.components.core.internal.Configuration
import com.adyen.checkout.core.Environment
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
internal class TestOnlineBankingConfiguration private constructor(
    override val shopperLocale: Locale,
    override val environment: Environment,
    override val clientKey: String,
    override val analyticsConfiguration: AnalyticsConfiguration?,
    override val amount: Amount?,
    override val isSubmitButtonVisible: Boolean?,
) : Configuration,
    ButtonConfiguration {

    class Builder : BaseConfigurationBuilder<TestOnlineBankingConfiguration, Builder>, ButtonConfigurationBuilder {

        private var isSubmitButtonVisible: Boolean? = null

        constructor(context: Context, environment: Environment, clientKey: String) : super(
            context,
            environment,
            clientKey
        )

        constructor(
            shopperLocale: Locale,
            environment: Environment,
            clientKey: String
        ) : super(shopperLocale, environment, clientKey)

        override fun setSubmitButtonVisible(isSubmitButtonVisible: Boolean): Builder {
            this.isSubmitButtonVisible = isSubmitButtonVisible
            return this
        }

        override fun buildInternal(): TestOnlineBankingConfiguration {
            return TestOnlineBankingConfiguration(
                shopperLocale = shopperLocale,
                environment = environment,
                clientKey = clientKey,
                analyticsConfiguration = analyticsConfiguration,
                amount = amount,
                isSubmitButtonVisible = isSubmitButtonVisible,
            )
        }
    }
}
