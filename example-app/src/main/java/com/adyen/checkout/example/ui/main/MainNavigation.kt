/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 26/4/2022.
 */

package com.adyen.checkout.example.ui.main

import com.adyen.checkout.components.core.PaymentMethodsApiResponse
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.sessions.core.CheckoutSession

internal sealed class MainNavigation {

    object Bacs : MainNavigation()

    object Blik : MainNavigation()

    object Card : MainNavigation()

    class Instant(val paymentMethodType: String) : MainNavigation()

    object CardWithSession : MainNavigation()

    object GiftCard : MainNavigation()

    object GiftCardWithSession : MainNavigation()

    object CardWithSessionTakenOver : MainNavigation()

    data class DropIn(
        val paymentMethodsApiResponse: PaymentMethodsApiResponse,
        val dropInConfiguration: DropInConfiguration
    ) : MainNavigation()

    data class DropInWithSession(
        val checkoutSession: CheckoutSession,
        val dropInConfiguration: DropInConfiguration
    ) : MainNavigation()

    data class DropInWithCustomSession(
        val checkoutSession: CheckoutSession,
        val dropInConfiguration: DropInConfiguration
    ) : MainNavigation()
}
