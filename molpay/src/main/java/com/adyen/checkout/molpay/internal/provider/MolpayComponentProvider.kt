/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 12/4/2022.
 */

package com.adyen.checkout.molpay.internal.provider

import androidx.annotation.RestrictTo
import com.adyen.checkout.action.DefaultActionHandlingComponent
import com.adyen.checkout.action.GenericActionDelegate
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.base.ComponentEventHandler
import com.adyen.checkout.components.base.ComponentParams
import com.adyen.checkout.components.model.payments.request.MolpayPaymentMethod
import com.adyen.checkout.issuerlist.internal.provider.IssuerListComponentProvider
import com.adyen.checkout.issuerlist.internal.ui.IssuerListDelegate
import com.adyen.checkout.molpay.MolpayComponent
import com.adyen.checkout.molpay.MolpayConfiguration
import com.adyen.checkout.sessions.model.setup.SessionSetupConfiguration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MolpayComponentProvider(
    overrideComponentParams: ComponentParams? = null,
    private val sessionSetupConfiguration: SessionSetupConfiguration? = null
) : IssuerListComponentProvider<MolpayComponent, MolpayConfiguration, MolpayPaymentMethod>(
    componentClass = MolpayComponent::class.java,
    overrideComponentParams = overrideComponentParams,
) {

    override fun createComponent(
        delegate: IssuerListDelegate<MolpayPaymentMethod>,
        genericActionDelegate: GenericActionDelegate,
        actionHandlingComponent: DefaultActionHandlingComponent,
        componentEventHandler: ComponentEventHandler<PaymentComponentState<MolpayPaymentMethod>>
    ) = MolpayComponent(
        delegate = delegate,
        genericActionDelegate = genericActionDelegate,
        actionHandlingComponent = actionHandlingComponent,
        componentEventHandler = componentEventHandler,
    )

    override fun createPaymentMethod() = MolpayPaymentMethod()

    override fun getSupportedPaymentMethods(): List<String> = MolpayComponent.PAYMENT_METHOD_TYPES
}