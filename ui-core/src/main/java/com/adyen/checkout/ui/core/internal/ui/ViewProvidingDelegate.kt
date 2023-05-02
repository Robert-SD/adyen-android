/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 31/8/2022.
 */

package com.adyen.checkout.ui.core.internal.ui

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.Flow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ViewProvidingDelegate {
    val viewFlow: Flow<ComponentViewType?>
}
