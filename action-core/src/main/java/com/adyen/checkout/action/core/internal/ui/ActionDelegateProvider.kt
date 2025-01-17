/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 24/8/2022.
 */

package com.adyen.checkout.action.core.internal.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.adyen.checkout.action.core.GenericActionConfiguration
import com.adyen.checkout.adyen3ds2.Adyen3DS2Configuration
import com.adyen.checkout.adyen3ds2.internal.provider.Adyen3DS2ComponentProvider
import com.adyen.checkout.await.AwaitConfiguration
import com.adyen.checkout.await.internal.provider.AwaitComponentProvider
import com.adyen.checkout.components.core.action.Action
import com.adyen.checkout.components.core.action.AwaitAction
import com.adyen.checkout.components.core.action.BaseThreeds2Action
import com.adyen.checkout.components.core.action.QrCodeAction
import com.adyen.checkout.components.core.action.RedirectAction
import com.adyen.checkout.components.core.action.SdkAction
import com.adyen.checkout.components.core.action.VoucherAction
import com.adyen.checkout.components.core.internal.BaseConfigurationBuilder
import com.adyen.checkout.components.core.internal.Configuration
import com.adyen.checkout.components.core.internal.ui.ActionDelegate
import com.adyen.checkout.components.core.internal.ui.model.ComponentParams
import com.adyen.checkout.components.core.internal.ui.model.SessionParams
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.internal.util.runCompileOnly
import com.adyen.checkout.qrcode.QRCodeConfiguration
import com.adyen.checkout.qrcode.internal.provider.QRCodeComponentProvider
import com.adyen.checkout.redirect.RedirectConfiguration
import com.adyen.checkout.redirect.internal.provider.RedirectComponentProvider
import com.adyen.checkout.voucher.VoucherConfiguration
import com.adyen.checkout.voucher.internal.provider.VoucherComponentProvider
import com.adyen.checkout.wechatpay.WeChatPayActionConfiguration
import com.adyen.checkout.wechatpay.internal.provider.WeChatPayActionComponentProvider

internal class ActionDelegateProvider(
    private val overrideComponentParams: ComponentParams?,
    private val overrideSessionParams: SessionParams?,
) {

    fun getDelegate(
        action: Action,
        configuration: GenericActionConfiguration,
        savedStateHandle: SavedStateHandle,
        application: Application,
    ): ActionDelegate {
        return when (action) {
            is AwaitAction -> {
                AwaitComponentProvider(overrideComponentParams, overrideSessionParams).getDelegate(
                    getConfigurationForAction(configuration),
                    savedStateHandle,
                    application
                )
            }

            is QrCodeAction -> {
                QRCodeComponentProvider(overrideComponentParams, overrideSessionParams).getDelegate(
                    getConfigurationForAction(configuration),
                    savedStateHandle,
                    application
                )
            }

            is RedirectAction -> {
                RedirectComponentProvider(overrideComponentParams, overrideSessionParams).getDelegate(
                    getConfigurationForAction(configuration),
                    savedStateHandle,
                    application
                )
            }

            is BaseThreeds2Action -> {
                Adyen3DS2ComponentProvider(overrideComponentParams, overrideSessionParams).getDelegate(
                    getConfigurationForAction(configuration),
                    savedStateHandle,
                    application
                )
            }

            is VoucherAction -> {
                VoucherComponentProvider(overrideComponentParams, overrideSessionParams).getDelegate(
                    getConfigurationForAction(configuration),
                    savedStateHandle,
                    application
                )
            }

            is SdkAction<*> -> {
                WeChatPayActionComponentProvider(overrideComponentParams, overrideSessionParams).getDelegate(
                    getConfigurationForAction(configuration),
                    savedStateHandle,
                    application
                )
            }

            else -> throw CheckoutException("Can't find delegate for action: ${action.type}")
        }
    }

    private inline fun <reified T : Configuration> getConfigurationForAction(
        configuration: GenericActionConfiguration
    ): T {
        return configuration.getConfigurationForAction() ?: getDefaultConfiguration(configuration)
    }

    private inline fun <reified T : Configuration> getDefaultConfiguration(
        configuration: Configuration
    ): T {
        val shopperLocale = configuration.shopperLocale
        val environment = configuration.environment
        val clientKey = configuration.clientKey

        val builder: BaseConfigurationBuilder<*, *> = when (T::class) {
            runCompileOnly { AwaitConfiguration::class } -> AwaitConfiguration.Builder(
                shopperLocale,
                environment,
                clientKey
            )

            runCompileOnly { RedirectConfiguration::class } -> RedirectConfiguration.Builder(
                shopperLocale,
                environment,
                clientKey
            )

            runCompileOnly { QRCodeConfiguration::class } -> QRCodeConfiguration.Builder(
                shopperLocale,
                environment,
                clientKey
            )

            runCompileOnly { Adyen3DS2Configuration::class } -> Adyen3DS2Configuration.Builder(
                shopperLocale,
                environment,
                clientKey
            )

            runCompileOnly { WeChatPayActionConfiguration::class } -> WeChatPayActionConfiguration.Builder(
                shopperLocale,
                environment,
                clientKey
            )

            runCompileOnly { VoucherConfiguration::class } -> VoucherConfiguration.Builder(
                shopperLocale,
                environment,
                clientKey
            )

            else -> throw CheckoutException("Unable to find component configuration for class - ${T::class}")
        }

        return builder.build() as T
    }
}
