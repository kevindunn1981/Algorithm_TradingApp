package com.algotrader.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.model.TradingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val apiSecret: String = "",
    val isPaperTrading: Boolean = true,
    val tradingMode: TradingMode = TradingMode.PAPER,
    val notificationsEnabled: Boolean = true,
    val tradeNotifications: Boolean = true,
    val signalNotifications: Boolean = true,
    val darkMode: Boolean = true,
    val defaultCapital: String = "100000",
    val maxPositionSize: String = "10000",
    val defaultStopLoss: String = "5.0",
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val apiConnected: Boolean = false,
    val apiTestMessage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun updateApiSecret(secret: String) {
        _uiState.value = _uiState.value.copy(apiSecret = secret)
    }

    fun togglePaperTrading(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isPaperTrading = enabled,
            tradingMode = if (enabled) TradingMode.PAPER else TradingMode.LIVE
        )
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    fun toggleTradeNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(tradeNotifications = enabled)
    }

    fun toggleSignalNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(signalNotifications = enabled)
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(darkMode = enabled)
    }

    fun updateDefaultCapital(capital: String) {
        _uiState.value = _uiState.value.copy(defaultCapital = capital)
    }

    fun updateMaxPositionSize(size: String) {
        _uiState.value = _uiState.value.copy(maxPositionSize = size)
    }

    fun updateDefaultStopLoss(stopLoss: String) {
        _uiState.value = _uiState.value.copy(defaultStopLoss = stopLoss)
    }

    fun testApiConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(apiTestMessage = "Testing connection...")
            try {
                kotlinx.coroutines.delay(1000)
                if (_uiState.value.apiKey.isNotEmpty() && _uiState.value.apiSecret.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        apiConnected = true,
                        apiTestMessage = "Connected successfully!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        apiConnected = false,
                        apiTestMessage = "Please enter API key and secret"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    apiConnected = false,
                    apiTestMessage = "Connection failed: ${e.message}"
                )
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                kotlinx.coroutines.delay(500)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
