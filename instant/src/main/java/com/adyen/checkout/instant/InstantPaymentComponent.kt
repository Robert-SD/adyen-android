package com.adyen.checkout.instant

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.checkout.action.internal.ActionHandlingComponent
import com.adyen.checkout.action.internal.DefaultActionHandlingComponent
import com.adyen.checkout.action.internal.ui.GenericActionDelegate
import com.adyen.checkout.components.core.internal.ComponentEventHandler
import com.adyen.checkout.components.core.internal.PaymentComponent
import com.adyen.checkout.components.core.internal.PaymentComponentEvent
import com.adyen.checkout.components.core.internal.toActionCallback
import com.adyen.checkout.components.core.internal.ui.ComponentDelegate
import com.adyen.checkout.core.internal.util.LogUtil
import com.adyen.checkout.core.internal.util.Logger
import com.adyen.checkout.instant.internal.provider.InstantPaymentComponentProvider
import com.adyen.checkout.instant.internal.ui.InstantPaymentDelegate
import com.adyen.checkout.ui.core.internal.ui.ComponentViewType
import com.adyen.checkout.ui.core.internal.ui.ViewableComponent
import kotlinx.coroutines.flow.Flow

/**
 * A [PaymentComponent] used for handling payment methods that do not require any input from the shopper.
 */
class InstantPaymentComponent internal constructor(
    private val instantPaymentDelegate: InstantPaymentDelegate,
    private val genericActionDelegate: GenericActionDelegate,
    private val actionHandlingComponent: DefaultActionHandlingComponent,
    internal val componentEventHandler: ComponentEventHandler<InstantComponentState>,
) : ViewModel(),
    PaymentComponent,
    ViewableComponent,
    ActionHandlingComponent by actionHandlingComponent {

    override val delegate: ComponentDelegate get() = actionHandlingComponent.activeDelegate

    override val viewFlow: Flow<ComponentViewType?> = genericActionDelegate.viewFlow

    init {
        instantPaymentDelegate.initialize(viewModelScope)
        genericActionDelegate.initialize(viewModelScope)
        componentEventHandler.initialize(viewModelScope)
    }

    internal fun observe(
        lifecycleOwner: LifecycleOwner,
        callback: (PaymentComponentEvent<InstantComponentState>) -> Unit
    ) {
        instantPaymentDelegate.observe(lifecycleOwner, viewModelScope, callback)
        genericActionDelegate.observe(lifecycleOwner, viewModelScope, callback.toActionCallback())
    }

    internal fun removeObserver() {
        instantPaymentDelegate.removeObserver()
        genericActionDelegate.removeObserver()
    }

    override fun setInteractionBlocked(isInteractionBlocked: Boolean) {
        // no ops
    }

    override fun onCleared() {
        super.onCleared()
        Logger.d(TAG, "onCleared")
        instantPaymentDelegate.onCleared()
        genericActionDelegate.onCleared()
        componentEventHandler.onCleared()
    }

    companion object {
        private val TAG = LogUtil.getTag()

        @JvmField
        val PROVIDER = InstantPaymentComponentProvider()
    }
}
