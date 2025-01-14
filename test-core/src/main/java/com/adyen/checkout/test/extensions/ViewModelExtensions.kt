/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 12/12/2022.
 */

package com.adyen.checkout.test.extensions

import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel

/**
 * Invokes the [ViewModel.onCleared] method. This method is protected, so we can only call it with reflection.
 *
 * Should only be used in tests.
 */
@RestrictTo(RestrictTo.Scope.TESTS)
fun ViewModel.invokeOnCleared() {
    with(javaClass.getDeclaredMethod("onCleared")) {
        isAccessible = true
        invoke(this@invokeOnCleared)
    }
}
