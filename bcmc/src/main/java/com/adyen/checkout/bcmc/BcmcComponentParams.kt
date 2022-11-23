/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 15/11/2022.
 */

package com.adyen.checkout.bcmc

import androidx.annotation.RestrictTo
import com.adyen.checkout.components.base.ComponentParams
import com.adyen.checkout.core.api.Environment
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BcmcComponentParams(
    override val shopperLocale: Locale,
    override val environment: Environment,
    override val clientKey: String,
    val isHolderNameRequired: Boolean,
    val shopperReference: String?,
    val isStorePaymentFieldVisible: Boolean,
) : ComponentParams