/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 11/8/2022.
 */

package com.adyen.checkout.card.internal.util

import com.adyen.checkout.card.internal.data.model.DetectedCardType

internal object DetectedCardTypesUtils {

    fun filterDetectedCardTypes(
        detectedCardTypes: List<DetectedCardType>,
        selectedCardIndex: Int
    ): List<DetectedCardType> {
        val supportedCardTypes = detectedCardTypes.filter { it.isSupported }
        val sortedCardTypes = DualBrandedCardUtils.sortBrands(supportedCardTypes)
        return markSelectedCard(sortedCardTypes, selectedCardIndex)
    }

    fun getSelectedOrFirstDetectedCardType(detectedCardTypes: List<DetectedCardType>): DetectedCardType? {
        return getSelectedCardType(detectedCardTypes) ?: detectedCardTypes.firstOrNull()
    }

    fun getSelectedCardType(detectedCardTypes: List<DetectedCardType>): DetectedCardType? {
        return detectedCardTypes.firstOrNull { it.isSelected }
    }

    private fun markSelectedCard(cards: List<DetectedCardType>, selectedIndex: Int): List<DetectedCardType> {
        if (cards.size <= SINGLE_CARD_LIST_SIZE) return cards
        return cards.mapIndexed { index, card ->
            if (index == selectedIndex) {
                card.copy(isSelected = true)
            } else {
                card
            }
        }
    }

    private const val SINGLE_CARD_LIST_SIZE = 1
}
