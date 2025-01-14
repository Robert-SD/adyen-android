/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 18/7/2022.
 */

package com.adyen.checkout.cse

import com.adyen.checkout.cse.internal.BaseCardEncrypter
import com.adyen.checkout.cse.internal.ClientSideEncrypter
import com.adyen.checkout.cse.internal.DateGenerator
import com.adyen.checkout.cse.internal.DefaultCardEncrypter
import com.adyen.checkout.cse.internal.DefaultGenericEncrypter

/**
 * Allows the encryption of card data to be sent to Adyen's APIs.
 * Use this class with the custom card integration.
 */
object CardEncrypter {

    private val encrypter = provideCardEncrypter()

    /**
     * Encrypts the available card data from [UnencryptedCard] into individual encrypted blocks.
     *
     * @param unencryptedCard The card data to be encrypted.
     * @param publicKey     The key to be used for encryption.
     * @return An [EncryptedCard] object with each encrypted field.
     * @throws EncryptionException in case the encryption fails.
     */
    @Throws(EncryptionException::class)
    fun encryptFields(
        unencryptedCard: UnencryptedCard,
        publicKey: String
    ): EncryptedCard {
        return encrypter.encryptFields(
            unencryptedCard = unencryptedCard,
            publicKey = publicKey
        )
    }

    /**
     * Encrypts all the card data present in [UnencryptedCard] into a single block of content.
     *
     * @param unencryptedCard The card data to be encrypted.
     * @param publicKey     The key to be used for encryption.
     * @return The encrypted card data String.
     * @throws EncryptionException in case the encryption fails.
     */
    @Throws(EncryptionException::class)
    fun encrypt(
        unencryptedCard: UnencryptedCard,
        publicKey: String
    ): String {
        return encrypter.encrypt(
            unencryptedCard = unencryptedCard,
            publicKey = publicKey
        )
    }

    /**
     * Encrypts the BIN of the card to be used in the Bin Lookup endpoint.
     *
     * @param bin The BIN value to be encrypted.
     * @param publicKey The key to be used for encryption.
     * @return The encrypted bin String.
     * @throws EncryptionException in case the encryption fails.
     */
    @Throws(EncryptionException::class)
    fun encryptBin(bin: String, publicKey: String): String {
        return encrypter.encryptBin(
            bin = bin,
            publicKey = publicKey
        )
    }

    private fun provideCardEncrypter(): BaseCardEncrypter {
        return DefaultCardEncrypter(DefaultGenericEncrypter(ClientSideEncrypter(), DateGenerator()))
    }
}
