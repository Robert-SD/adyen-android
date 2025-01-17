/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 4/6/2019.
 */
package com.adyen.checkout.components.core.action

import com.adyen.checkout.components.core.internal.ActionTypes
import com.adyen.checkout.core.exception.ModelSerializationException
import com.adyen.checkout.core.internal.data.model.getStringOrNull
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

@Parcelize
data class RedirectAction(
    override var type: String? = null,
    override var paymentData: String? = null,
    override var paymentMethodType: String? = null,
    var method: String? = null,
    var url: String? = null,
) : Action() {

    companion object {
        const val ACTION_TYPE = ActionTypes.REDIRECT
        private const val METHOD = "method"
        private const val URL = "url"

        @JvmField
        val SERIALIZER: Serializer<RedirectAction> = object : Serializer<RedirectAction> {
            override fun serialize(modelObject: RedirectAction): JSONObject {
                return try {
                    JSONObject().apply {
                        putOpt(TYPE, modelObject.type)
                        putOpt(PAYMENT_DATA, modelObject.paymentData)
                        putOpt(PAYMENT_METHOD_TYPE, modelObject.paymentMethodType)
                        putOpt(METHOD, modelObject.method)
                        putOpt(URL, modelObject.url)
                    }
                } catch (e: JSONException) {
                    throw ModelSerializationException(RedirectAction::class.java, e)
                }
            }

            override fun deserialize(jsonObject: JSONObject): RedirectAction {
                return RedirectAction(
                    type = jsonObject.getStringOrNull(TYPE),
                    paymentData = jsonObject.getStringOrNull(PAYMENT_DATA),
                    paymentMethodType = jsonObject.getStringOrNull(PAYMENT_METHOD_TYPE),
                    method = jsonObject.getStringOrNull(METHOD),
                    url = jsonObject.getStringOrNull(URL),
                )
            }
        }
    }
}
