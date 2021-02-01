/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 29/1/2021.
 */

package com.adyen.checkout.card.data

import com.adyen.checkout.card.model.Brand

data class DetectedCardType(
    val cardType: CardType,
    val isReliable: Boolean,
    val showExpiryDate: Boolean,
    val enableLuhnCheck: Boolean,
    val cvcPolicy: Brand.CvcPolicy
)
