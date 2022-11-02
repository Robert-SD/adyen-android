/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 13/4/2021.
 */
package com.adyen.checkout.qrcode

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.checkout.components.ActionComponent
import com.adyen.checkout.components.ActionComponentEvent
import com.adyen.checkout.components.ActionComponentProvider
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.base.IntentHandlingComponent
import com.adyen.checkout.components.flow.mapToCallbackWithLifeCycle
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.components.ui.ViewableComponent
import com.adyen.checkout.components.ui.view.ComponentViewType
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

class QRCodeComponent(
    override val configuration: QRCodeConfiguration,
    override val delegate: QRCodeDelegate,
) : ViewModel(),
    ActionComponent<QRCodeConfiguration>,
    IntentHandlingComponent,
    ViewableComponent {

    override val viewFlow: Flow<ComponentViewType?> get() = delegate.viewFlow

    init {
        delegate.initialize(viewModelScope)
    }

    private var observerJobs: MutableList<Job> = mutableListOf()

    override fun observe(lifecycleOwner: LifecycleOwner, callback: (ActionComponentEvent) -> Unit) {
        removeObserver()
        delegate.detailsFlow.mapToCallbackWithLifeCycle(lifecycleOwner, viewModelScope, observerJobs) {
            callback(ActionComponentEvent.ActionDetails(it))
        }

        delegate.exceptionFlow.mapToCallbackWithLifeCycle(lifecycleOwner, viewModelScope, observerJobs) {
            callback(ActionComponentEvent.Error(ComponentError(it)))
        }

        // Immediately request a new status if the user resumes the app
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                delegate.refreshStatus()
            }
        })
    }

    override fun canHandleAction(action: Action): Boolean {
        return PROVIDER.canHandleAction(action)
    }

    override fun handleAction(action: Action, activity: Activity) {
        delegate.handleAction(action, activity)
    }

    /**
     * Call this method when receiving the return URL from the redirect with the result data.
     * This result will be in the [Intent.getData] and begins with the returnUrl you specified on the payments/ call.
     *
     * @param intent The received [Intent].
     */
    override fun handleIntent(intent: Intent) {
        delegate.handleIntent(intent)
    }

    override fun onCleared() {
        super.onCleared()
        Logger.d(TAG, "onCleared")
        delegate.onCleared()
        removeObserver()
    }

    override fun removeObserver() {
        if (observerJobs.isEmpty()) return
        Logger.d(TAG, "cleaning up existing observer")
        observerJobs.forEach { it.cancel() }
        observerJobs.clear()
    }

    companion object {
        private val TAG = LogUtil.getTag()

        @JvmField
        val PROVIDER: ActionComponentProvider<QRCodeComponent, QRCodeConfiguration, QRCodeDelegate> =
            QRCodeComponentProvider()
    }
}
