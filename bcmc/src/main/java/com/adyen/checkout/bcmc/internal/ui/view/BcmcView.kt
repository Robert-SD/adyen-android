/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 29/9/2022.
 */

package com.adyen.checkout.bcmc.internal.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.adyen.checkout.bcmc.R
import com.adyen.checkout.bcmc.databinding.BcmcViewBinding
import com.adyen.checkout.bcmc.internal.ui.BcmcDelegate
import com.adyen.checkout.bcmc.internal.ui.model.BcmcOutputData
import com.adyen.checkout.components.core.internal.ui.ComponentDelegate
import com.adyen.checkout.components.core.internal.ui.model.Validation
import com.adyen.checkout.ui.core.internal.ui.ComponentView
import com.adyen.checkout.ui.core.internal.util.hideError
import com.adyen.checkout.ui.core.internal.util.isVisible
import com.adyen.checkout.ui.core.internal.util.setLocalizedHintFromStyle
import com.adyen.checkout.ui.core.internal.util.setLocalizedTextFromStyle
import com.adyen.checkout.ui.core.internal.util.showError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Suppress("TooManyFunctions")
internal class BcmcView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr),
    ComponentView {

    private val binding = BcmcViewBinding.inflate(LayoutInflater.from(context), this)

    private lateinit var localizedContext: Context

    private lateinit var delegate: BcmcDelegate

    init {
        orientation = VERTICAL
        val padding = resources.getDimension(R.dimen.standard_margin).toInt()
        setPadding(padding, padding, padding, 0)
    }

    override fun initView(delegate: ComponentDelegate, coroutineScope: CoroutineScope, localizedContext: Context) {
        require(delegate is BcmcDelegate) { "Unsupported delegate type" }
        this.delegate = delegate

        this.localizedContext = localizedContext
        initLocalizedStrings(localizedContext)

        observeDelegate(delegate, coroutineScope)

        initCardNumberInput()
        initExpiryDateInput()
        initCardHolderInput()
        initStorePaymentMethodSwitch()
    }

    private fun initLocalizedStrings(localizedContext: Context) {
        with(binding) {
            textInputLayoutCardNumber.setLocalizedHintFromStyle(
                R.style.AdyenCheckout_Card_CardNumberInput,
                localizedContext
            )
            textInputLayoutExpiryDate.setLocalizedHintFromStyle(
                R.style.AdyenCheckout_Card_ExpiryDateInput,
                localizedContext
            )
            binding.textInputLayoutCardHolder.setLocalizedHintFromStyle(
                R.style.AdyenCheckout_Card_HolderNameInput,
                localizedContext
            )
            switchStorePaymentMethod.setLocalizedTextFromStyle(
                R.style.AdyenCheckout_Card_StorePaymentSwitch,
                localizedContext
            )
        }
    }

    private fun observeDelegate(delegate: BcmcDelegate, coroutineScope: CoroutineScope) {
        delegate.outputDataFlow
            .onEach { outputDataChanged(it) }
            .launchIn(coroutineScope)
    }

    private fun outputDataChanged(bcmcOutputData: BcmcOutputData) {
        setStorePaymentSwitchVisibility(bcmcOutputData.showStorePaymentField)
    }

    private fun setStorePaymentSwitchVisibility(showStorePaymentField: Boolean) {
        binding.switchStorePaymentMethod.isVisible = showStorePaymentField
    }

    private fun initExpiryDateInput() {
        binding.editTextExpiryDate.setOnChangeListener {
            delegate.updateInputData { expiryDate = binding.editTextExpiryDate.date }
            binding.textInputLayoutExpiryDate.hideError()
        }

        binding.editTextExpiryDate.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            val expiryDateValidation = delegate.outputData.expiryDateField.validation
            if (hasFocus) {
                binding.textInputLayoutExpiryDate.hideError()
            } else if (expiryDateValidation is Validation.Invalid) {
                val errorReasonResId = expiryDateValidation.reason
                binding.textInputLayoutExpiryDate.showError(localizedContext.getString(errorReasonResId))
            }
        }
    }

    private fun initCardNumberInput() {
        binding.editTextCardNumber.setOnChangeListener {
            delegate.updateInputData { cardNumber = binding.editTextCardNumber.rawValue }
            setCardNumberError(null)
        }

        binding.editTextCardNumber.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            val cardNumberValidation = delegate.outputData.cardNumberField.validation
            if (hasFocus) {
                setCardNumberError(null)
            } else if (cardNumberValidation is Validation.Invalid) {
                val errorReasonResId = cardNumberValidation.reason
                setCardNumberError(errorReasonResId)
            }
        }
    }

    private fun initCardHolderInput() {
        binding.textInputLayoutCardHolder.isVisible = delegate.componentParams.isHolderNameRequired
        binding.editTextCardHolder.setOnChangeListener {
            delegate.updateInputData { cardHolderName = binding.editTextCardHolder.rawValue }
            binding.textInputLayoutCardHolder.hideError()
        }

        binding.editTextCardHolder.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            val cardHolderValidation = delegate.outputData.cardHolderNameField.validation
            if (hasFocus) {
                binding.textInputLayoutCardHolder.hideError()
            } else if (cardHolderValidation is Validation.Invalid) {
                val errorReasonResId = cardHolderValidation.reason
                binding.textInputLayoutCardHolder.showError(localizedContext.getString(errorReasonResId))
            }
        }
    }

    private fun initStorePaymentMethodSwitch() {
        binding.switchStorePaymentMethod.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            delegate.updateInputData { isStorePaymentMethodSwitchChecked = isChecked }
        }
    }

    override fun highlightValidationErrors() {
        val outputData = delegate.outputData

        var isErrorFocused = false
        val cardNumberValidation = outputData.cardNumberField.validation
        if (cardNumberValidation is Validation.Invalid) {
            isErrorFocused = true
            binding.editTextCardNumber.requestFocus()
            val errorReasonResId = cardNumberValidation.reason
            setCardNumberError(errorReasonResId)
        }

        val expiryFieldValidation = outputData.expiryDateField.validation
        if (expiryFieldValidation is Validation.Invalid) {
            if (!isErrorFocused) {
                binding.textInputLayoutExpiryDate.requestFocus()
            }
            val errorReasonResId = expiryFieldValidation.reason
            binding.textInputLayoutExpiryDate.showError(localizedContext.getString(errorReasonResId))
        }

        val cardHolderNameValidation = outputData.cardHolderNameField.validation
        if (cardHolderNameValidation is Validation.Invalid) {
            if (!isErrorFocused) {
                binding.textInputLayoutCardHolder.requestFocus()
            }
            val errorReasonResId = cardHolderNameValidation.reason
            binding.textInputLayoutCardHolder.showError(localizedContext.getString(errorReasonResId))
        }
    }

    private fun setCardNumberError(@StringRes stringResId: Int?) {
        if (stringResId == null) {
            binding.textInputLayoutCardNumber.hideError()
            binding.cardBrandLogoImageView.isVisible = true
        } else {
            binding.textInputLayoutCardNumber.showError(localizedContext.getString(stringResId))
            binding.cardBrandLogoImageView.isVisible = false
        }
    }

    override fun getView(): View = this
}
