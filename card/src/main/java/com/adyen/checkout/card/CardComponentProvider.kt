/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 23/7/2019.
 */
package com.adyen.checkout.card

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.adyen.checkout.card.data.CardType
import com.adyen.checkout.card.repository.AddressRepository
import com.adyen.checkout.card.delegate.DetectCardTypeDelegate
import com.adyen.checkout.card.repository.BinLookupRepository
import com.adyen.checkout.components.StoredPaymentComponentProvider
import com.adyen.checkout.components.base.GenericPaymentMethodDelegate
import com.adyen.checkout.components.base.GenericStoredPaymentDelegate
import com.adyen.checkout.components.base.lifecycle.viewModelFactory
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.paymentmethods.StoredPaymentMethod
import com.adyen.checkout.components.repository.DefaultPublicKeyRepository
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.cse.DefaultCardEncrypter

private val TAG = LogUtil.getTag()

class CardComponentProvider : StoredPaymentComponentProvider<CardComponent, CardConfiguration> {

    override fun get(
        savedStateRegistryOwner: SavedStateRegistryOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        paymentMethod: PaymentMethod,
        configuration: CardConfiguration,
        defaultArgs: Bundle?
    ): CardComponent {
        val verifiedConfiguration = checkSupportedCardTypes(paymentMethod, configuration)
        val cardEncrypter = DefaultCardEncrypter()
        val binLookupRepository = BinLookupRepository(cardEncrypter)
        val detectCardTypeDelegate = DetectCardTypeDelegate(binLookupRepository)
        val publicKeyRepository = DefaultPublicKeyRepository()
        val addressRepository = AddressRepository()
        val cardValidationMapper = CardValidationMapper()
        val factory = viewModelFactory(savedStateRegistryOwner, defaultArgs) { savedStateHandle ->
            CardComponent(
                savedStateHandle,
                GenericPaymentMethodDelegate(paymentMethod),
                DefaultCardDelegate(
                    publicKeyRepository,
                    verifiedConfiguration,
                    paymentMethod,
                    addressRepository,
                    detectCardTypeDelegate,
                    cardValidationMapper,
                    cardEncrypter
                ),
                verifiedConfiguration
            )
        }
        return ViewModelProvider(viewModelStoreOwner, factory).get(CardComponent::class.java)
    }

    override fun get(
        savedStateRegistryOwner: SavedStateRegistryOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        storedPaymentMethod: StoredPaymentMethod,
        configuration: CardConfiguration,
        defaultArgs: Bundle?
    ): CardComponent {
        val publicKeyRepository = DefaultPublicKeyRepository()
        val factory = viewModelFactory(savedStateRegistryOwner, defaultArgs) { savedStateHandle ->
            CardComponent(
                savedStateHandle,
                GenericStoredPaymentDelegate(storedPaymentMethod),
                StoredCardDelegate(
                    storedPaymentMethod,
                    configuration,
                    DefaultCardEncrypter(),
                    publicKeyRepository,
                ),
                configuration
            )
        }
        return ViewModelProvider(viewModelStoreOwner, factory).get(CardComponent::class.java)
    }

    /**
     * Check which set of supported cards to pass to the component.
     * Priority is: Custom -> PaymentMethod.brands -> Default
     *
     * @param paymentMethod The payment methods object that will start the component.
     * @param cardConfiguration The configuration object that will start the component.
     * @return The Configuration object with possibly adjusted values.
     */
    private fun checkSupportedCardTypes(
        paymentMethod: PaymentMethod,
        cardConfiguration: CardConfiguration
    ): CardConfiguration {
        if (cardConfiguration.supportedCardTypes.isNotEmpty()) {
            return cardConfiguration
        }

        val brands = paymentMethod.brands
        var supportedCardTypes = CardConfiguration.DEFAULT_SUPPORTED_CARDS_LIST

        // Get card types from brands in PaymentMethod object
        if (!brands.isNullOrEmpty()) {
            supportedCardTypes = arrayListOf()
            for (brand in brands) {
                val brandType = CardType.getByBrandName(brand)
                if (brandType != null) {
                    supportedCardTypes.add(brandType)
                } else {
                    Logger.e(TAG, "Failed to get card type for brand: $brand")
                }
            }
        } else {
            Logger.d(TAG, "Falling back to DEFAULT_SUPPORTED_CARDS_LIST")
        }
        @Suppress("SpreadOperator")
        return cardConfiguration.newBuilder()
            .setSupportedCardTypes(*supportedCardTypes.toTypedArray())
            .build()
    }
}
