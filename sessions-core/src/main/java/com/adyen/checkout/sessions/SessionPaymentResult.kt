/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 23/1/2023.
 */

package com.adyen.checkout.sessions

import android.os.Parcelable
import com.adyen.checkout.components.core.OrderResponse
import kotlinx.parcelize.Parcelize

// TODO SESSIONS: docs
@Parcelize
data class SessionPaymentResult(
    val sessionResult: String?,
    val sessionData: String?,
    val resultCode: String?,
    val order: OrderResponse?,
) : Parcelable
