/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 19/11/2021.
 */

package com.adyen.checkout.dropin.internal.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.adyen.checkout.bacs.BacsDirectDebitComponent
import com.adyen.checkout.core.internal.util.LogUtil
import com.adyen.checkout.core.internal.util.Logger
import com.adyen.checkout.dropin.databinding.FragmentBacsDirectDebitComponentBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

internal class BacsDirectDebitDialogFragment : BaseComponentDialogFragment() {

    private var _binding: FragmentBacsDirectDebitComponentBinding? = null
    private val binding: FragmentBacsDirectDebitComponentBinding get() = requireNotNull(_binding)

    private val bacsDirectDebitComponent: BacsDirectDebitComponent by lazy { component as BacsDirectDebitComponent }

    companion object : BaseCompanion<BacsDirectDebitDialogFragment>(BacsDirectDebitDialogFragment::class.java) {
        private val TAG = LogUtil.getTag()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBacsDirectDebitComponentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d(TAG, "onViewCreated")
        binding.header.text = paymentMethod.name

        binding.bacsView.attach(bacsDirectDebitComponent, viewLifecycleOwner)

        if (bacsDirectDebitComponent.isConfirmationRequired()) {
            setInitViewState(BottomSheetBehavior.STATE_EXPANDED)
            binding.bacsView.requestFocus()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Logger.d(TAG, "onCreateDialog")
        val dialog = super.onCreateDialog(savedInstanceState)
        setDialogToFullScreen(dialog)
        return dialog
    }

    override fun onBackPressed(): Boolean {
        return if (bacsDirectDebitComponent.handleBackPress()) {
            true
        } else {
            super.onBackPressed()
        }
    }

    private fun setDialogToFullScreen(dialog: Dialog) {
        dialog.setOnShowListener {
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            val layoutParams = bottomSheet?.layoutParams
            val behavior = bottomSheet?.let { BottomSheetBehavior.from(it) }
            behavior?.isDraggable = false
            layoutParams?.height = WindowManager.LayoutParams.MATCH_PARENT
            bottomSheet?.layoutParams = layoutParams
            behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
