/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 14/2/2023.
 */

package com.adyen.checkout.dropin

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.adyen.checkout.dropin.ui.DropInActivity

internal class SessionDropInResultContract :
    ActivityResultContract<SessionDropInResultContractParams, SessionDropInResult?>() {
    override fun createIntent(context: Context, input: SessionDropInResultContractParams): Intent {
        return DropInActivity.createIntent(
            context = context,
            dropInConfiguration = input.dropInConfiguration,
            checkoutSession = input.checkoutSession,
            service = ComponentName(context, input.serviceClass),
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SessionDropInResult? {
        return handleActivityResult(resultCode, intent)
    }

    private fun handleActivityResult(resultCode: Int, data: Intent?): SessionDropInResult? {
        return when {
            data == null -> null
            resultCode == Activity.RESULT_CANCELED && data.hasExtra(DropIn.ERROR_REASON_KEY) -> {
                val reason = data.getStringExtra(DropIn.ERROR_REASON_KEY) ?: ""
                if (reason == DropIn.ERROR_REASON_USER_CANCELED) {
                    SessionDropInResult.CancelledByUser()
                } else {
                    SessionDropInResult.Error(reason)
                }
            }
            resultCode == Activity.RESULT_OK && data.hasExtra(DropIn.RESULT_KEY) -> {
                SessionDropInResult.Finished(data.getStringExtra(DropIn.RESULT_KEY) ?: "")
            }
            else -> null
        }
    }
}
