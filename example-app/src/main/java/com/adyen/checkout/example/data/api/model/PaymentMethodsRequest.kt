/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/10/2019.
 */

package com.adyen.checkout.example.data.api.model

import androidx.annotation.Keep
import com.adyen.checkout.components.core.Amount
import com.adyen.checkout.components.core.OrderRequest

@Keep
data class PaymentMethodsRequest(
    val merchantAccount: String,
    val shopperReference: String,
    val amount: Amount?,
    val countryCode: String,
    val shopperLocale: String,
    val channel: String,
    val splitCardFundingSources: Boolean,
    val order: OrderRequest?,
)
