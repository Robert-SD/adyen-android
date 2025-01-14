/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 30/11/2022.
 */

package com.adyen.checkout.dropin.internal.ui.model

import com.adyen.checkout.components.core.Amount
import com.adyen.checkout.components.core.internal.ui.model.AnalyticsParams
import com.adyen.checkout.dropin.DropInConfiguration

internal class DropInComponentParamsMapper {

    fun mapToParams(
        dropInConfiguration: DropInConfiguration,
        overrideAmount: Amount?,
    ): DropInComponentParams {
        with(dropInConfiguration) {
            return DropInComponentParams(
                shopperLocale = shopperLocale,
                environment = environment,
                clientKey = clientKey,
                analyticsParams = AnalyticsParams(analyticsConfiguration),
                isCreatedByDropIn = true,
                amount = overrideAmount,
                showPreselectedStoredPaymentMethod = showPreselectedStoredPaymentMethod,
                skipListWhenSinglePaymentMethod = skipListWhenSinglePaymentMethod,
                isRemovingStoredPaymentMethodsEnabled = isRemovingStoredPaymentMethodsEnabled,
                additionalDataForDropInService = additionalDataForDropInService,
            )
        }
    }
}
