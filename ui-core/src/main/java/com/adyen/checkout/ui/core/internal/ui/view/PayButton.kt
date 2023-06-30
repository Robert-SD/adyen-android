/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 30/6/2023.
 */

package com.adyen.checkout.ui.core.internal.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

abstract class PayButton(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : FrameLayout(context, attrs, defStyleAttr) {

    abstract override fun setOnClickListener(listener: OnClickListener?)

    abstract fun setText(text: String?)
}
