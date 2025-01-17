/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/10/2019.
 */

package com.adyen.checkout.example.ui.main

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.DropInResult
import com.adyen.checkout.dropin.SessionDropInResult
import com.adyen.checkout.example.data.storage.KeyValueStorage
import com.adyen.checkout.example.extensions.getLogTag
import com.adyen.checkout.example.repositories.PaymentsRepository
import com.adyen.checkout.example.service.getPaymentMethodRequest
import com.adyen.checkout.example.service.getSessionRequest
import com.adyen.checkout.example.service.getSettingsInstallmentOptionsMode
import com.adyen.checkout.example.ui.configuration.CheckoutConfigurationProvider
import com.adyen.checkout.sessions.core.CheckoutSession
import com.adyen.checkout.sessions.core.CheckoutSessionProvider
import com.adyen.checkout.sessions.core.CheckoutSessionResult
import com.adyen.checkout.sessions.core.SessionModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val paymentsRepository: PaymentsRepository,
    private val keyValueStorage: KeyValueStorage,
    private val checkoutConfigurationProvider: CheckoutConfigurationProvider,
) : ViewModel() {

    private val _useSessions: MutableStateFlow<Boolean> = MutableStateFlow(keyValueStorage.useSessions())
    private val _showLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _mainViewState: MutableStateFlow<MainViewState> = MutableStateFlow(getViewState())
    val mainViewState: Flow<MainViewState> = _mainViewState

    private val _eventFlow: MutableSharedFlow<MainEvent> = MutableSharedFlow(extraBufferCapacity = 1)
    val eventFlow: Flow<MainEvent> = _eventFlow

    init {
        _useSessions.onEach {
            loadViewState()
        }.launchIn(viewModelScope)

        _showLoading.onEach {
            loadViewState()
        }.launchIn(viewModelScope)
    }

    internal fun onResume() {
        viewModelScope.launch {
            loadViewState()
        }
    }

    fun onComponentEntryClick(entry: ComponentItem.Entry) {
        when (entry) {
            is ComponentItem.Entry.Bacs -> _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.Bacs))
            is ComponentItem.Entry.Blik -> _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.Blik))
            is ComponentItem.Entry.Card -> _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.Card))
            is ComponentItem.Entry.Klarna -> _eventFlow.tryEmit(
                MainEvent.NavigateTo(MainNavigation.Instant(PAYMENT_METHOD_KLARNA))
            )

            is ComponentItem.Entry.PayPal ->
                _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.Instant(PAYMENT_METHOD_PAYPAL)))

            is ComponentItem.Entry.Instant ->
                _eventFlow.tryEmit(
                    MainEvent.NavigateTo(MainNavigation.Instant(keyValueStorage.getInstantPaymentMethodType()))
                )

            is ComponentItem.Entry.CardWithSession ->
                _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.CardWithSession))

            is ComponentItem.Entry.CardWithSessionTakenOver ->
                _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.CardWithSessionTakenOver))

            is ComponentItem.Entry.GiftCard -> _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.GiftCard))
            is ComponentItem.Entry.GiftCardWithSession ->
                _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.GiftCardWithSession))

            is ComponentItem.Entry.DropIn -> startDropInFlow()
            is ComponentItem.Entry.DropInWithSession -> startSessionDropInFlow(false)
            is ComponentItem.Entry.DropInWithCustomSession -> startSessionDropInFlow(true)
        }
    }

    private fun startDropInFlow() {
        viewModelScope.launch {
            showLoading(true)

            val paymentMethods = getPaymentMethods()

            showLoading(false)

            if (paymentMethods != null) {
                val dropInConfiguration = checkoutConfigurationProvider.getDropInConfiguration()
                _eventFlow.tryEmit(MainEvent.NavigateTo(MainNavigation.DropIn(paymentMethods, dropInConfiguration)))
            } else {
                onError("Something went wrong while fetching payment methods")
            }
        }
    }

    private fun startSessionDropInFlow(takeOverSession: Boolean) {
        viewModelScope.launch {
            showLoading(true)

            val dropInConfiguration = checkoutConfigurationProvider.getDropInConfiguration()

            val session = getSession(dropInConfiguration)

            showLoading(false)

            if (session != null) {
                val navigation = if (takeOverSession) {
                    MainNavigation.DropInWithCustomSession(session, dropInConfiguration)
                } else {
                    MainNavigation.DropInWithSession(session, dropInConfiguration)
                }
                _eventFlow.tryEmit(MainEvent.NavigateTo(navigation))
            } else {
                onError("Something went wrong while starting session")
            }
        }
    }

    private suspend fun getPaymentMethods() = paymentsRepository.getPaymentMethods(
        getPaymentMethodRequest(
            merchantAccount = keyValueStorage.getMerchantAccount(),
            shopperReference = keyValueStorage.getShopperReference(),
            amount = keyValueStorage.getAmount(),
            countryCode = keyValueStorage.getCountry(),
            shopperLocale = keyValueStorage.getShopperLocale(),
            splitCardFundingSources = keyValueStorage.isSplitCardFundingSources(),
        )
    )

    private suspend fun getSession(dropInConfiguration: DropInConfiguration): CheckoutSession? {
        val sessionModel = paymentsRepository.createSession(
            getSessionRequest(
                merchantAccount = keyValueStorage.getMerchantAccount(),
                shopperReference = keyValueStorage.getShopperReference(),
                amount = keyValueStorage.getAmount(),
                countryCode = keyValueStorage.getCountry(),
                shopperLocale = keyValueStorage.getShopperLocale(),
                splitCardFundingSources = keyValueStorage.isSplitCardFundingSources(),
                isExecuteThreeD = keyValueStorage.isExecuteThreeD(),
                isThreeds2Enabled = keyValueStorage.isThreeds2Enabled(),
                redirectUrl = savedStateHandle.get<String>(MainActivity.RETURN_URL_EXTRA)
                    ?: error("Return url should be set"),
                shopperEmail = keyValueStorage.getShopperEmail(),
                installmentOptions = getSettingsInstallmentOptionsMode(keyValueStorage.getInstallmentOptionsMode())
            )
        ) ?: return null

        return getCheckoutSession(sessionModel, dropInConfiguration)
    }

    private suspend fun getCheckoutSession(
        sessionModel: SessionModel,
        dropInConfiguration: DropInConfiguration
    ): CheckoutSession? {
        return when (val result = CheckoutSessionProvider.createSession(sessionModel, dropInConfiguration)) {
            is CheckoutSessionResult.Success -> result.checkoutSession
            is CheckoutSessionResult.Error -> {
                onError("Something went wrong while starting session")
                null
            }
        }
    }

    private fun onError(message: String) {
        _eventFlow.tryEmit(MainEvent.Toast(message))
    }

    fun onSessionsToggled(enable: Boolean) {
        viewModelScope.launch {
            keyValueStorage.setUseSessions(enable)
            _useSessions.emit(enable)
        }
    }

    private suspend fun showLoading(loading: Boolean) {
        _showLoading.emit(loading)
    }

    private suspend fun loadViewState() {
        _mainViewState.emit(getViewState())
    }

    private fun getViewState(): MainViewState {
        val useSessions = _useSessions.value
        val showLoading = _showLoading.value
        return MainViewState(
            listItems = getListItems(useSessions),
            useSessions = useSessions,
            showLoading = showLoading,
        )
    }

    private fun getListItems(useSessions: Boolean): List<ComponentItem> {
        return if (useSessions) {
            ComponentItemProvider.getSessionItems()
        } else {
            val instantPaymentMethodType = keyValueStorage.getInstantPaymentMethodType()
            ComponentItemProvider.getDefaultItems(instantPaymentMethodType)
        }
    }

    fun onDropInResult(dropInResult: DropInResult?) {
        val message = when (dropInResult) {
            is DropInResult.CancelledByUser -> "Canceled by user"
            is DropInResult.Error -> dropInResult.reason ?: "DropInResult is error without reason"
            is DropInResult.Finished -> dropInResult.result
            null -> "DropInResult is null"
        }
        _eventFlow.tryEmit(MainEvent.Toast(message))
    }

    fun onDropInResult(sessionDropInResult: SessionDropInResult?) {
        val message = when (sessionDropInResult) {
            is SessionDropInResult.CancelledByUser -> "Canceled by user"
            is SessionDropInResult.Error -> sessionDropInResult.reason ?: "DropInResult is error without reason"
            is SessionDropInResult.Finished -> sessionDropInResult.result.resultCode ?: "Result code is null"
            null -> "DropInResult is null"
        }
        _eventFlow.tryEmit(MainEvent.Toast(message))
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared")
    }

    companion object {
        private val TAG = getLogTag()
        private const val PAYMENT_METHOD_PAYPAL = "paypal"
        private const val PAYMENT_METHOD_KLARNA = "klarna"
    }
}
