/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 26/10/2021.
 */

package com.adyen.checkout.card.internal.util

import android.content.Context
import com.adyen.checkout.card.CardBrand
import com.adyen.checkout.card.InstallmentConfiguration
import com.adyen.checkout.card.InstallmentOptions
import com.adyen.checkout.card.R
import com.adyen.checkout.card.internal.ui.model.InstallmentOption
import com.adyen.checkout.card.internal.ui.model.InstallmentOptionParams
import com.adyen.checkout.card.internal.ui.model.InstallmentParams
import com.adyen.checkout.card.internal.ui.view.InstallmentModel
import com.adyen.checkout.components.core.Installments

private const val REVOLVING_INSTALLMENT_VALUE = 1

internal object InstallmentUtils {

    /**
     * Create a list of installment options from [InstallmentParams].
     */
    fun makeInstallmentOptions(
        params: InstallmentParams?,
        cardBrand: CardBrand?,
        isCardTypeReliable: Boolean
    ): List<InstallmentModel> {
        val hasCardBasedInstallmentOptions = params?.cardBasedOptions != null
        val hasDefaultInstallmentOptions = params?.defaultOptions != null
        val hasOptionsForCardType = hasCardBasedInstallmentOptions &&
            isCardTypeReliable &&
            (params?.cardBasedOptions?.any { it.cardBrand == cardBrand } ?: false)

        return when {
            hasOptionsForCardType -> {
                makeInstallmentModelList(params?.cardBasedOptions?.firstOrNull { it.cardBrand == cardBrand })
            }
            hasDefaultInstallmentOptions -> {
                makeInstallmentModelList(params?.defaultOptions)
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun makeInstallmentModelList(installmentOptions: InstallmentOptionParams?): List<InstallmentModel> {
        if (installmentOptions == null) return emptyList()
        val installmentOptionsList = mutableListOf<InstallmentModel>()
        val oneTimeOption = InstallmentModel(
            textResId = R.string.checkout_card_installments_option_one_time,
            value = null,
            option = InstallmentOption.ONE_TIME
        )
        installmentOptionsList.add(oneTimeOption)

        if (installmentOptions.includeRevolving) {
            val revolvingOption = InstallmentModel(
                textResId = R.string.checkout_card_installments_option_revolving,
                value = REVOLVING_INSTALLMENT_VALUE,
                option = InstallmentOption.REVOLVING
            )
            installmentOptionsList.add(revolvingOption)
        }

        val regularOptions = installmentOptions.values.map {
            InstallmentModel(
                textResId = R.string.checkout_card_installments_option_regular,
                value = it,
                option = InstallmentOption.REGULAR
            )
        }
        installmentOptionsList.addAll(regularOptions)
        return installmentOptionsList
    }

    /**
     * Get the text to be shown for different types of [InstallmentOption].
     */
    fun getTextForInstallmentOption(context: Context, installmentModel: InstallmentModel?): String {
        return when (installmentModel?.option) {
            InstallmentOption.REGULAR -> context.getString(installmentModel.textResId, installmentModel.value)
            InstallmentOption.REVOLVING, InstallmentOption.ONE_TIME -> context.getString(installmentModel.textResId)
            else -> ""
        }
    }

    /**
     * Populate the [Installments] model object from [InstallmentModel].
     */
    fun makeInstallmentModelObject(installmentModel: InstallmentModel?): Installments? {
        return when (installmentModel?.option) {
            InstallmentOption.REGULAR, InstallmentOption.REVOLVING -> {
                Installments(installmentModel.option.type, installmentModel.value)
            }
            else -> null
        }
    }

    /**
     * Check whether the card based options contain only one option defined per card type.
     */
    fun isCardBasedOptionsValid(
        cardBasedInstallmentOptions: List<InstallmentOptions.CardBasedInstallmentOptions>?
    ): Boolean {
        val hasMultipleOptionsForSameCard = cardBasedInstallmentOptions
            ?.groupBy { it.cardBrand }
            ?.values
            ?.any { it.size > 1 } ?: false
        return !hasMultipleOptionsForSameCard
    }

    /**
     * Check whether [InstallmentOptions.values] in installment options defined in
     * [InstallmentConfiguration] are valid (i.e. all the values are greater than 1).
     */
    fun areInstallmentValuesValid(installmentConfiguration: InstallmentConfiguration): Boolean {
        val installmentOptions = mutableListOf<InstallmentOptions?>()
        installmentOptions.add(installmentConfiguration.defaultOptions)
        installmentOptions.addAll(installmentConfiguration.cardBasedOptions)
        val hasInvalidValue = installmentOptions.filterNotNull().any { it.values.any { it <= 1 } }
        return !hasInvalidValue
    }
}
