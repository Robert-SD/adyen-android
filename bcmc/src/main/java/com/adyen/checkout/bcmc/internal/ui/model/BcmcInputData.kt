/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 27/8/2020.
 */
package com.adyen.checkout.bcmc.internal.ui.model

import com.adyen.checkout.card.internal.ui.model.ExpiryDate
import com.adyen.checkout.components.core.internal.ui.model.InputData

internal data class BcmcInputData(
    var cardNumber: String = "",
    var expiryDate: ExpiryDate = ExpiryDate.EMPTY_DATE,
    var cardHolderName: String = "",
    var isStorePaymentMethodSwitchChecked: Boolean = false,
) : InputData
