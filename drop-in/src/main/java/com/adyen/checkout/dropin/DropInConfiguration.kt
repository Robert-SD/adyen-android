/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 21/3/2019.
 */

package com.adyen.checkout.dropin

import android.content.Context
import android.os.Bundle
import com.adyen.checkout.ach.ACHDirectDebitConfiguration
import com.adyen.checkout.action.core.GenericActionConfiguration
import com.adyen.checkout.action.core.internal.ActionHandlingPaymentMethodConfigurationBuilder
import com.adyen.checkout.bacs.BacsDirectDebitConfiguration
import com.adyen.checkout.bcmc.BcmcConfiguration
import com.adyen.checkout.blik.BlikConfiguration
import com.adyen.checkout.boleto.BoletoConfiguration
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.cashapppay.CashAppPayConfiguration
import com.adyen.checkout.components.core.Amount
import com.adyen.checkout.components.core.AnalyticsConfiguration
import com.adyen.checkout.components.core.PaymentMethodTypes
import com.adyen.checkout.components.core.internal.Configuration
import com.adyen.checkout.conveniencestoresjp.ConvenienceStoresJPConfiguration
import com.adyen.checkout.core.Environment
import com.adyen.checkout.dotpay.DotpayConfiguration
import com.adyen.checkout.dropin.DropInConfiguration.Builder
import com.adyen.checkout.entercash.EntercashConfiguration
import com.adyen.checkout.eps.EPSConfiguration
import com.adyen.checkout.googlepay.GooglePayConfiguration
import com.adyen.checkout.ideal.IdealConfiguration
import com.adyen.checkout.mbway.MBWayConfiguration
import com.adyen.checkout.molpay.MolpayConfiguration
import com.adyen.checkout.onlinebankingcz.OnlineBankingCZConfiguration
import com.adyen.checkout.onlinebankingjp.OnlineBankingJPConfiguration
import com.adyen.checkout.onlinebankingpl.OnlineBankingPLConfiguration
import com.adyen.checkout.onlinebankingsk.OnlineBankingSKConfiguration
import com.adyen.checkout.openbanking.OpenBankingConfiguration
import com.adyen.checkout.payeasy.PayEasyConfiguration
import com.adyen.checkout.sepa.SepaConfiguration
import com.adyen.checkout.seveneleven.SevenElevenConfiguration
import com.adyen.checkout.upi.UPIConfiguration
import kotlinx.parcelize.Parcelize
import java.util.Locale
import kotlin.collections.set

/**
 * This is the base configuration for the Drop-In solution. You need to use the [Builder] to instantiate this class.
 * There you will find specific methods to add configurations for each specific component, to be able to customize
 * their behavior.
 * If you don't specify anything, a default configuration will be used.
 */
@Parcelize
@Suppress("LongParameterList")
class DropInConfiguration private constructor(
    override val shopperLocale: Locale,
    override val environment: Environment,
    override val clientKey: String,
    override val analyticsConfiguration: AnalyticsConfiguration?,
    override val amount: Amount?,
    private val availablePaymentConfigs: HashMap<String, Configuration>,
    internal val genericActionConfiguration: GenericActionConfiguration,
    val showPreselectedStoredPaymentMethod: Boolean,
    val skipListWhenSinglePaymentMethod: Boolean,
    val isRemovingStoredPaymentMethodsEnabled: Boolean,
    val additionalDataForDropInService: Bundle?,
) : Configuration {

    internal fun <T : Configuration> getConfigurationForPaymentMethod(paymentMethod: String): T? {
        if (availablePaymentConfigs.containsKey(paymentMethod)) {
            @Suppress("UNCHECKED_CAST")
            return availablePaymentConfigs[paymentMethod] as T
        }
        return null
    }

    /**
     * Builder for creating a [DropInConfiguration] where you can set specific Configurations for a Payment Method
     */
    @Suppress("unused", "TooManyFunctions")
    class Builder :
        ActionHandlingPaymentMethodConfigurationBuilder<DropInConfiguration, Builder> {

        private val availablePaymentConfigs = HashMap<String, Configuration>()

        private var showPreselectedStoredPaymentMethod: Boolean = true
        private var skipListWhenSinglePaymentMethod: Boolean = false
        private var isRemovingStoredPaymentMethodsEnabled: Boolean = false
        private var additionalDataForDropInService: Bundle? = null

        /**
         * Create a [DropInConfiguration]
         *
         * @param shopperLocale The [Locale] of the shopper.
         * @param environment The [Environment] to be used for internal network calls from the SDK to Adyen.
         * @param clientKey Your Client Key used for internal network calls from the SDK to Adyen.
         */
        constructor(shopperLocale: Locale, environment: Environment, clientKey: String) : super(
            shopperLocale,
            environment,
            clientKey
        )

        /**
         * Alternative constructor that uses the [context] to fetch the user locale and use it as a shopper locale.
         *
         * @param context A context
         * @param environment The [Environment] to be used for internal network calls from the SDK to Adyen.
         * @param clientKey Your Client Key used for internal network calls from the SDK to Adyen.
         */
        constructor(context: Context, environment: Environment, clientKey: String) : super(
            context,
            environment,
            clientKey
        )

        /**
         * When set to false, Drop-in will skip the preselected screen and go straight to the payment methods list.
         */
        fun setShowPreselectedStoredPaymentMethod(showStoredPaymentMethod: Boolean): Builder {
            this.showPreselectedStoredPaymentMethod = showStoredPaymentMethod
            return this
        }

        /**
         * When set to true, Drop-in will skip the payment methods list screen if there is only a single payment method
         * available and no storedpayment methods.
         *
         * This only applies to payment methods that require a component (user input). Which means redirect payment
         * methods, SDK payment methods, etc will not be skipped even if this flag is set to true and a single payment
         * method is present.
         */
        fun setSkipListWhenSinglePaymentMethod(skipListWhenSinglePaymentMethod: Boolean): Builder {
            this.skipListWhenSinglePaymentMethod = skipListWhenSinglePaymentMethod
            return this
        }

        /**
         * When set to true, users can remove their stored payment methods by swiping left on the corresponding row in
         * the payment methods screen.
         *
         * You need to implement [DropInService.onRemoveStoredPaymentMethod] to handle the removal.
         */
        fun setEnableRemovingStoredPaymentMethods(isEnabled: Boolean): Builder {
            this.isRemovingStoredPaymentMethodsEnabled = isEnabled
            return this
        }

        /**
         * Pass a custom Bundle to Drop-in. This Bundle will passed to the [DropInService] and can be read using
         * [DropInService.getAdditionalData].
         */
        fun setAdditionalDataForDropInService(additionalDataForDropInService: Bundle): Builder {
            this.additionalDataForDropInService = additionalDataForDropInService
            return this
        }

        /**
         * Add configuration for Credit Card payment method.
         */
        fun addCardConfiguration(cardConfiguration: CardConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.SCHEME] = cardConfiguration
            return this
        }

        /**
         * Add configuration for Cash App Pay payment method.
         */
        fun addCashAppPayConfiguration(cashAppPayConfiguration: CashAppPayConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.CASH_APP_PAY] = cashAppPayConfiguration
            return this
        }

        /**
         * Add configuration for iDeal payment method.
         */
        fun addIdealConfiguration(idealConfiguration: IdealConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.IDEAL] = idealConfiguration
            return this
        }

        /**
         * Add configuration for MolPay Thailand payment method.
         */
        fun addMolpayThailandConfiguration(molpayConfiguration: MolpayConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.MOLPAY_THAILAND] = molpayConfiguration
            return this
        }

        /**
         * Add configuration for MolPay Malasya payment method.
         */
        fun addMolpayMalasyaConfiguration(molpayConfiguration: MolpayConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.MOLPAY_MALAYSIA] = molpayConfiguration
            return this
        }

        /**
         * Add configuration for MolPay Vietnam payment method.
         */
        fun addMolpayVietnamConfiguration(molpayConfiguration: MolpayConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.MOLPAY_VIETNAM] = molpayConfiguration
            return this
        }

        /**
         * Add configuration for DotPay payment method.
         */
        fun addDotpayConfiguration(dotpayConfiguration: DotpayConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.DOTPAY] = dotpayConfiguration
            return this
        }

        /**
         * Add configuration for Online Banking Czech Republic payment method.
         */
        fun addOnlineBankingCZConfiguration(onlineBankingCZConfiguration: OnlineBankingCZConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ONLINE_BANKING_CZ] = onlineBankingCZConfiguration
            return this
        }

        /**
         * Add configuration for Online Banking Poland payment method.
         */
        fun addOnlineBankingPLConfiguration(onlineBankingPLConfiguration: OnlineBankingPLConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ONLINE_BANKING_PL] = onlineBankingPLConfiguration
            return this
        }

        /**
         * Add configuration for Online Banking Slovakia payment method.
         */
        fun addOnlineBankingSKConfiguration(onlineBankingSKConfiguration: OnlineBankingSKConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ONLINE_BANKING_SK] = onlineBankingSKConfiguration
            return this
        }

        /**
         * Add configuration for EPS payment method.
         */
        fun addEpsConfiguration(epsConfiguration: EPSConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.EPS] = epsConfiguration
            return this
        }

        /**
         * Add configuration for EnterCash payment method.
         */
        fun addEntercashConfiguration(entercashConfiguration: EntercashConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ENTERCASH] = entercashConfiguration
            return this
        }

        /**
         * Add configuration for Open Banking payment method.
         */
        fun addOpenBankingConfiguration(openBankingConfiguration: OpenBankingConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.OPEN_BANKING] = openBankingConfiguration
            return this
        }

        /**
         * Add configuration for Google Pay payment method.
         */
        fun addGooglePayConfiguration(googlePayConfiguration: GooglePayConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.GOOGLE_PAY] = googlePayConfiguration
            availablePaymentConfigs[PaymentMethodTypes.GOOGLE_PAY_LEGACY] = googlePayConfiguration
            return this
        }

        /**
         * Add configuration for Sepa payment method.
         */
        fun addSepaConfiguration(sepaConfiguration: SepaConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.SEPA] = sepaConfiguration
            return this
        }

        /**
         * Add configuration for BCMC payment method.
         */
        fun addBcmcConfiguration(bcmcConfiguration: BcmcConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.BCMC] = bcmcConfiguration
            return this
        }

        /**
         * Add configuration for MB WAY payment method.
         */
        fun addMBWayConfiguration(mbwayConfiguration: MBWayConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.MB_WAY] = mbwayConfiguration
            return this
        }

        /**
         * Add configuration for Blik payment method.
         */
        fun addBlikConfiguration(blikConfiguration: BlikConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.BLIK] = blikConfiguration
            return this
        }

        /**
         * Add configuration for BACS Direct Debit payment method.
         */
        fun addBacsDirectDebitConfiguration(bacsDirectDebitConfiguration: BacsDirectDebitConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.BACS] = bacsDirectDebitConfiguration
            return this
        }

        /**
         * Add configuration for Seven Eleven payment method.
         */
        fun addSevenElevenConfiguration(sevenElevenConfiguration: SevenElevenConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ECONTEXT_SEVEN_ELEVEN] = sevenElevenConfiguration
            return this
        }

        /**
         * Add configuration for Online Banking Japan payment method.
         */
        fun addOnlineBankingJPConfiguration(onlineBankingJPConfiguration: OnlineBankingJPConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ECONTEXT_ONLINE] = onlineBankingJPConfiguration
            return this
        }

        /**
         * Add configuration for Convenience Stores Japan payment method.
         */
        fun addConvenienceStoresJPConfiguration(
            convenienceStoresJPConfiguration: ConvenienceStoresJPConfiguration
        ): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ECONTEXT_STORES] = convenienceStoresJPConfiguration
            return this
        }

        /**
         * Add configuration for Pay Easy payment method.
         */
        fun addPayEasyConfiguration(payEasyConfiguration: PayEasyConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ECONTEXT_ATM] = payEasyConfiguration
            return this
        }

        /**
         * Add configuration for ACH Direct Debit payment method.
         */
        fun addAchDirectDebitConfiguration(achDirectDebitConfiguration: ACHDirectDebitConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.ACH] = achDirectDebitConfiguration
            return this
        }

        /**
         * Add configuration for UPI payment method.
         */
        fun addUPIConfiguration(upiConfiguration: UPIConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.UPI] = upiConfiguration
            availablePaymentConfigs[PaymentMethodTypes.UPI_COLLECT] = upiConfiguration
            availablePaymentConfigs[PaymentMethodTypes.UPI_QR] = upiConfiguration
            return this
        }

        /**
         * Add configuration for Boleto payment method.
         */
        fun addBoletoConfiguration(boletoConfiguration: BoletoConfiguration): Builder {
            availablePaymentConfigs[PaymentMethodTypes.BOLETOBANCARIO] = boletoConfiguration
            availablePaymentConfigs[PaymentMethodTypes.BOLETOBANCARIO_BANCODOBRASIL] = boletoConfiguration
            availablePaymentConfigs[PaymentMethodTypes.BOLETOBANCARIO_BRADESCO] = boletoConfiguration
            availablePaymentConfigs[PaymentMethodTypes.BOLETOBANCARIO_HSBC] = boletoConfiguration
            availablePaymentConfigs[PaymentMethodTypes.BOLETOBANCARIO_ITAU] = boletoConfiguration
            availablePaymentConfigs[PaymentMethodTypes.BOLETOBANCARIO_SANTANDER] = boletoConfiguration
            availablePaymentConfigs[PaymentMethodTypes.BOLETO_PRIMEIRO_PAY] = boletoConfiguration
            return this
        }

        override fun buildInternal(): DropInConfiguration {
            return DropInConfiguration(
                shopperLocale = shopperLocale,
                environment = environment,
                clientKey = clientKey,
                analyticsConfiguration = analyticsConfiguration,
                availablePaymentConfigs = availablePaymentConfigs,
                genericActionConfiguration = genericActionConfigurationBuilder.build(),
                amount = amount,
                showPreselectedStoredPaymentMethod = showPreselectedStoredPaymentMethod,
                skipListWhenSinglePaymentMethod = skipListWhenSinglePaymentMethod,
                isRemovingStoredPaymentMethodsEnabled = isRemovingStoredPaymentMethodsEnabled,
                additionalDataForDropInService = additionalDataForDropInService,
            )
        }
    }
}
