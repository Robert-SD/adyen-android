/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 2/7/2019.
 */

package com.adyen.checkout.dropin.internal.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.adyen.checkout.components.core.StoredPaymentMethod
import com.adyen.checkout.core.internal.util.LogUtil
import com.adyen.checkout.core.internal.util.Logger
import com.adyen.checkout.dropin.R
import com.adyen.checkout.dropin.databinding.FragmentPaymentMethodsListBinding
import com.adyen.checkout.dropin.internal.ui.model.PaymentMethodHeader
import com.adyen.checkout.dropin.internal.ui.model.PaymentMethodModel
import com.adyen.checkout.dropin.internal.ui.model.StoredPaymentMethodModel
import com.adyen.checkout.dropin.internal.util.getViewModel
import com.adyen.checkout.ui.core.internal.ui.view.AdyenSwipeToRevealLayout
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private val TAG = LogUtil.getTag()

@Suppress("TooManyFunctions")
internal class PaymentMethodListDialogFragment :
    DropInBottomSheetDialogFragment(),
    PaymentMethodAdapter.OnPaymentMethodSelectedCallback,
    PaymentMethodAdapter.OnStoredPaymentRemovedCallback {

    private var _binding: FragmentPaymentMethodsListBinding? = null
    private val binding: FragmentPaymentMethodsListBinding get() = requireNotNull(_binding)

    private lateinit var paymentMethodsListViewModel: PaymentMethodsListViewModel

    private var paymentMethodAdapter: PaymentMethodAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Logger.d(TAG, "onAttach")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Logger.d(TAG, "onCreateView")
        paymentMethodsListViewModel = getViewModel {
            PaymentMethodsListViewModel(
                requireActivity().application,
                dropInViewModel.getPaymentMethods(),
                dropInViewModel.getStoredPaymentMethods(),
                dropInViewModel.currentOrder,
                dropInViewModel.dropInConfiguration,
                dropInViewModel.amount
            )
        }
        _binding = FragmentPaymentMethodsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d(TAG, "onViewCreated")

        initPaymentMethodsRecyclerView()
        initObservers()
    }

    private fun initPaymentMethodsRecyclerView() {
        paymentMethodAdapter = PaymentMethodAdapter(this, this) {
            collapseNotUsedUnderlayButtons(binding.recyclerViewPaymentMethods, it)
        }

        binding.recyclerViewPaymentMethods.adapter = paymentMethodAdapter
    }

    private fun initObservers() {
        paymentMethodsListViewModel
            .paymentMethodsFlow
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach { paymentMethods ->
                Logger.d(TAG, "paymentMethods changed")
                paymentMethodAdapter?.submitList(paymentMethods)
            }.launchIn(lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        paymentMethodAdapter = null
        binding.recyclerViewPaymentMethods.adapter = null
        _binding = null
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Logger.d(TAG, "onCancel")
        protocol.terminateDropIn()
    }

    override fun onBackPressed(): Boolean {
        if (dropInViewModel.shouldShowPreselectedStored()) {
            protocol.showPreselectedDialog()
        } else {
            protocol.terminateDropIn()
        }
        return true
    }

    override fun onStoredPaymentMethodSelected(storedPaymentMethodModel: StoredPaymentMethodModel) {
        Logger.d(TAG, "onStoredPaymentMethodSelected")
        val storedPaymentMethod = dropInViewModel.getStoredPaymentMethod(storedPaymentMethodModel.id)
        protocol.showStoredComponentDialog(storedPaymentMethod, false)
    }

    override fun onPaymentMethodSelected(paymentMethod: PaymentMethodModel) {
        Logger.d(TAG, "onPaymentMethodSelected - ${paymentMethod.type}")

        protocol.showComponentDialog(paymentMethodsListViewModel.getPaymentMethod(paymentMethod))
    }

    override fun onHeaderActionSelected(header: PaymentMethodHeader) {
        when (header.type) {
            PaymentMethodHeader.TYPE_GIFT_CARD_HEADER -> showCancelOrderAlert()
        }
    }

    override fun onStoredPaymentMethodRemoved(storedPaymentMethodModel: StoredPaymentMethodModel) {
        val storedPaymentMethod = StoredPaymentMethod(
            id = storedPaymentMethodModel.id
        )
        protocol.removeStoredPaymentMethod(storedPaymentMethod)
    }

    fun removeStoredPaymentMethod(id: String) {
        paymentMethodsListViewModel.removePaymentMethodWithId(id)
    }

    private fun showCancelOrderAlert() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.checkout_giftcard_remove_gift_cards_title)
            .setMessage(R.string.checkout_giftcard_remove_gift_cards_body)
            .setNegativeButton(R.string.checkout_giftcard_remove_gift_cards_negative_button) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.checkout_giftcard_remove_gift_cards_positive_button) { dialog, _ ->
                dialog.dismiss()
                protocol.requestOrderCancellation()
            }
            .show()
    }

    private fun collapseNotUsedUnderlayButtons(recyclerView: RecyclerView, draggedItem: AdyenSwipeToRevealLayout) {
        recyclerView.children.filterIsInstance(AdyenSwipeToRevealLayout::class.java).forEach {
            if (it != draggedItem) {
                it.collapseUnderlay()
            }
        }
    }
}