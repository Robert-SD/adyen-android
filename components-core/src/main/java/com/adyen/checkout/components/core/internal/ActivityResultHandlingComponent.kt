/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 27/5/2021.
 */

package com.adyen.checkout.components.core.internal

import android.app.Activity
import android.content.Intent
import androidx.annotation.RestrictTo

/**
 * A component that expects to receive and handle an activity result.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ActivityResultHandlingComponent : ResultHandlingComponent {

    /**
     * Handles the result of an activity result.
     * Call this method inside your [Activity.onActivityResult] after validating the request code.
     */
    fun handleActivityResult(resultCode: Int, data: Intent?)
}
