/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 25/4/2019.
 */
package com.adyen.checkout.issuerlist.internal.ui.model

import androidx.annotation.RestrictTo
import com.adyen.checkout.components.core.internal.ui.model.OutputData

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class IssuerListOutputData(
    val selectedIssuer: IssuerModel?,
) : OutputData {

    override val isValid = selectedIssuer != null
}
