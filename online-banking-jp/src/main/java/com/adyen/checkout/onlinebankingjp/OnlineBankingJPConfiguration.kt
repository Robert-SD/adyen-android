/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 24/1/2023.
 */

package com.adyen.checkout.onlinebankingjp

import android.content.Context
import com.adyen.checkout.action.core.GenericActionConfiguration
import com.adyen.checkout.components.core.Amount
import com.adyen.checkout.core.Environment
import com.adyen.checkout.econtext.internal.EContextConfiguration
import kotlinx.parcelize.Parcelize
import java.util.Locale

/**
 * Configuration class for the [OnlineBankingJPComponent].
 */
@Suppress("LongParameterList")
@Parcelize
class OnlineBankingJPConfiguration private constructor(
    override val shopperLocale: Locale,
    override val environment: Environment,
    override val clientKey: String,
    override val isAnalyticsEnabled: Boolean?,
    override val amount: Amount,
    override val isSubmitButtonVisible: Boolean?,
    override val genericActionConfiguration: GenericActionConfiguration,
) : EContextConfiguration() {

    /**
     * Builder to create an [OnlineBankingJPConfiguration].
     */
    class Builder : EContextConfiguration.Builder<OnlineBankingJPConfiguration, Builder> {

        /**
         * Alternative constructor that uses the [context] to fetch the user locale and use it as a shopper locale.
         *
         * @param context A context
         * @param environment The [Environment] to be used for internal network calls from the SDK to Adyen.
         * @param clientKey Your Client Key used for internal network calls from the SDK to Adyen.
         */
        constructor(context: Context, environment: Environment, clientKey: String) : super(
            context,
            environment,
            clientKey
        )

        /**
         * Initialize a configuration builder with the required fields.
         *
         * @param shopperLocale The [Locale] of the shopper.
         * @param environment The [Environment] to be used for internal network calls from the SDK to Adyen.
         * @param clientKey Your Client Key used for internal network calls from the SDK to Adyen.
         */
        constructor(
            shopperLocale: Locale,
            environment: Environment,
            clientKey: String
        ) : super(shopperLocale, environment, clientKey)

        override fun buildInternal(): OnlineBankingJPConfiguration {
            return OnlineBankingJPConfiguration(
                shopperLocale = shopperLocale,
                environment = environment,
                clientKey = clientKey,
                isAnalyticsEnabled = isAnalyticsEnabled,
                amount = amount,
                isSubmitButtonVisible = isSubmitButtonVisible,
                genericActionConfiguration = genericActionConfigurationBuilder.build(),
            )
        }
    }
}
