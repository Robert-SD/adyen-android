/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 28/11/2022.
 */

package com.adyen.checkout.dropin.internal.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.adyen.checkout.components.core.Amount
import com.adyen.checkout.components.core.PaymentMethodsApiResponse
import com.adyen.checkout.components.core.internal.SavedStateHandleContainer
import com.adyen.checkout.components.core.internal.SavedStateHandleProperty
import com.adyen.checkout.core.internal.util.LogUtil
import com.adyen.checkout.core.internal.util.Logger
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.internal.ui.model.OrderModel
import com.adyen.checkout.giftcard.GiftCardComponentState
import com.adyen.checkout.sessions.core.CheckoutSession
import com.adyen.checkout.sessions.core.internal.data.model.SessionDetails
import com.adyen.checkout.sessions.core.internal.data.model.mapToDetails

internal class DropInSavedStateHandleContainer(
    override val savedStateHandle: SavedStateHandle,
) : SavedStateHandleContainer {

    var dropInConfiguration: DropInConfiguration? by SavedStateHandleProperty(DROP_IN_CONFIGURATION_KEY)
    var serviceComponentName: ComponentName? by SavedStateHandleProperty(DROP_IN_SERVICE_KEY)
    var amount: Amount? by SavedStateHandleProperty(AMOUNT)
    var sessionDetails: SessionDetails? by SavedStateHandleProperty(SESSION_KEY)
    var isSessionsFlowTakenOver: Boolean? by SavedStateHandleProperty(IS_SESSIONS_FLOW_TAKEN_OVER_KEY)
    var paymentMethodsApiResponse: PaymentMethodsApiResponse? by SavedStateHandleProperty(PAYMENT_METHODS_RESPONSE_KEY)
    var isWaitingResult: Boolean? by SavedStateHandleProperty(IS_WAITING_FOR_RESULT_KEY)
    var cachedGiftCardComponentState: GiftCardComponentState? by SavedStateHandleProperty(CACHED_GIFT_CARD)
    var cachedPartialPaymentAmount: Amount? by SavedStateHandleProperty(PARTIAL_PAYMENT_AMOUNT)
    var currentOrder: OrderModel? by SavedStateHandleProperty(CURRENT_ORDER)
    var packageName: String? by SavedStateHandleProperty(PACKAGE_NAME_KEY)
}

internal object DropInBundleHandler {
    private val TAG = LogUtil.getTag()

    fun putIntentExtras(
        intent: Intent,
        dropInConfiguration: DropInConfiguration,
        paymentMethodsApiResponse: PaymentMethodsApiResponse,
        service: ComponentName,
        packageName: String,
    ) {
        intent.apply {
            putExtra(PAYMENT_METHODS_RESPONSE_KEY, paymentMethodsApiResponse)
            putExtra(DROP_IN_CONFIGURATION_KEY, dropInConfiguration)
            putExtra(DROP_IN_SERVICE_KEY, service)
            putExtra(PACKAGE_NAME_KEY, packageName)
        }
    }

    fun putIntentExtras(
        intent: Intent,
        dropInConfiguration: DropInConfiguration,
        checkoutSession: CheckoutSession,
        service: ComponentName,
        packageName: String,
    ) {
        putIntentExtras(
            intent,
            dropInConfiguration,
            checkoutSession.sessionSetupResponse.paymentMethods ?: PaymentMethodsApiResponse(),
            service,
            packageName,
        )
        intent.apply {
            putExtra(SESSION_KEY, checkoutSession.sessionSetupResponse.mapToDetails())
        }
    }

    fun assertBundleExists(bundle: Bundle?): Boolean {
        return when {
            bundle == null -> {
                Logger.e(TAG, "Failed to initialize - bundle is null")
                false
            }
            !bundle.containsKey(DROP_IN_SERVICE_KEY) || !bundle.containsKey(DROP_IN_CONFIGURATION_KEY) -> {
                Logger.e(TAG, "Failed to initialize - bundle does not have the required keys")
                false
            }
            else -> true
        }
    }
}

private const val PAYMENT_METHODS_RESPONSE_KEY = "PAYMENT_METHODS_RESPONSE_KEY"
private const val SESSION_KEY = "SESSION_KEY"
private const val IS_SESSIONS_FLOW_TAKEN_OVER_KEY = "IS_SESSIONS_FLOW_TAKEN_OVER_KEY"
private const val DROP_IN_CONFIGURATION_KEY = "DROP_IN_CONFIGURATION_KEY"
private const val DROP_IN_SERVICE_KEY = "DROP_IN_SERVICE_KEY"
private const val IS_WAITING_FOR_RESULT_KEY = "IS_WAITING_FOR_RESULT_KEY"
private const val PACKAGE_NAME_KEY = "PACKAGE_NAME_KEY"
private const val CACHED_GIFT_CARD = "CACHED_GIFT_CARD"
private const val CURRENT_ORDER = "CURRENT_ORDER"
private const val PARTIAL_PAYMENT_AMOUNT = "PARTIAL_PAYMENT_AMOUNT"
private const val AMOUNT = "AMOUNT"