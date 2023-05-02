/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/12/2019.
 */
package com.adyen.checkout.components.core

import com.adyen.checkout.core.exception.ModelSerializationException
import com.adyen.checkout.core.internal.data.model.ModelObject
import com.adyen.checkout.core.internal.data.model.getStringOrNull
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

@Parcelize
data class ShopperName(
    var firstName: String? = null,
    var infix: String? = null,
    var lastName: String? = null,
    var gender: String? = null,
) : ModelObject() {

    companion object {
        private const val FIRST_NAME = "firstName"
        private const val INFIX = "infix"
        private const val LAST_NAME = "lastName"
        private const val GENDER = "gender"

        @JvmField
        val SERIALIZER: Serializer<ShopperName> = object : Serializer<ShopperName> {
            override fun serialize(modelObject: ShopperName): JSONObject {
                return try {
                    JSONObject().apply {
                        putOpt(FIRST_NAME, modelObject.firstName)
                        putOpt(INFIX, modelObject.infix)
                        putOpt(LAST_NAME, modelObject.lastName)
                        putOpt(GENDER, modelObject.gender)
                    }
                } catch (e: JSONException) {
                    throw ModelSerializationException(ShopperName::class.java, e)
                }
            }

            override fun deserialize(jsonObject: JSONObject): ShopperName {
                return ShopperName(
                    firstName = jsonObject.getStringOrNull(FIRST_NAME),
                    infix = jsonObject.getStringOrNull(INFIX),
                    lastName = jsonObject.getStringOrNull(LAST_NAME),
                    gender = jsonObject.getStringOrNull(GENDER),
                )
            }
        }
    }
}
