/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 12/4/2022.
 */

package com.adyen.checkout.dotpay

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.adyen.checkout.components.PaymentComponentProvider
import com.adyen.checkout.components.base.lifecycle.get
import com.adyen.checkout.components.base.lifecycle.viewModelFactory
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.payments.request.DotpayPaymentMethod
import com.adyen.checkout.issuerlist.DefaultIssuerListDelegate

class DotpayComponentProvider : PaymentComponentProvider<DotpayComponent, DotpayConfiguration> {

    override fun get(
        savedStateRegistryOwner: SavedStateRegistryOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        paymentMethod: PaymentMethod,
        configuration: DotpayConfiguration,
        defaultArgs: Bundle?,
        key: String?,
    ): DotpayComponent {
        val genericFactory: ViewModelProvider.Factory =
            viewModelFactory(savedStateRegistryOwner, defaultArgs) { savedStateHandle ->
                val delegate = DefaultIssuerListDelegate(configuration, paymentMethod) { DotpayPaymentMethod() }
                DotpayComponent(savedStateHandle, delegate, configuration)
            }
        return ViewModelProvider(viewModelStoreOwner, genericFactory)[key, DotpayComponent::class.java]
    }
}
