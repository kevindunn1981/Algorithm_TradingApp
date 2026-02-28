package com.algotrader.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.model.TradingMode
import com.algotrader.app.data.remote.broker.BrokerInfo
import com.algotrader.app.data.remote.broker.BrokerManager
import com.algotrader.app.data.remote.broker.BrokerType
import com.algotrader.app.data.remote.broker.MoomooBrokerProvider
import com.algotrader.app.data.remote.moomoo.MoomooConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Broker selection
    val selectedBroker: BrokerType = BrokerType.ALPACA,
    val availableBrokers: List<BrokerInfo> = emptyList(),

    // Alpaca settings
    val apiKey: String = "",
    val apiSecret: String = "",

    // Moomoo settings
    val moomooHost: String = "127.0.0.1",
    val moomooPort: String = "33333",
    val moomooAccountId: String = "",
    val moomooMarket: Int = MoomooConstants.TRD_MARKET_US,
    val moomooUseWebSocket: Boolean = true,

    // Common settings
    val isPaperTrading: Boolean = true,
    val tradingMode: TradingMode = TradingMode.PAPER,
    val notificationsEnabled: Boolean = true,
    val tradeNotifications: Boolean = true,
    val signalNotifications: Boolean = true,
    val darkMode: Boolean = true,
    val defaultCapital: String = "100000",
    val maxPositionSize: String = "10000",
    val defaultStopLoss: String = "5.0",

    // Connection state
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val apiConnected: Boolean = false,
    val apiTestMessage: String = "",
    val isTestingConnection: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val brokerManager: BrokerManager,
    private val moomooProvider: MoomooBrokerProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(
        availableBrokers = brokerManager.availableBrokers,
        selectedBroker = brokerManager.activeBrokerType.value
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val marketOptions = listOf(
        MoomooConstants.TRD_MARKET_US to "US Stocks",
        MoomooConstants.TRD_MARKET_HK to "Hong Kong",
        MoomooConstants.TRD_MARKET_CN to "China A-Shares",
        MoomooConstants.TRD_MARKET_SG to "Singapore",
        MoomooConstants.TRD_MARKET_JP to "Japan",
        MoomooConstants.TRD_MARKET_AU to "Australia"
    )

    fun selectBroker(type: BrokerType) {
        _uiState.value = _uiState.value.copy(
            selectedBroker = type,
            apiTestMessage = "",
            apiConnected = false
        )
        brokerManager.switchBroker(type)
    }

    // ── Alpaca ──────────────────────────────────────────────────────────

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun updateApiSecret(secret: String) {
        _uiState.value = _uiState.value.copy(apiSecret = secret)
    }

    // ── Moomoo ─────────────────────────────────────────────────────────

    fun updateMoomooHost(host: String) {
        _uiState.value = _uiState.value.copy(moomooHost = host)
    }

    fun updateMoomooPort(port: String) {
        _uiState.value = _uiState.value.copy(moomooPort = port)
    }

    fun updateMoomooAccountId(id: String) {
        _uiState.value = _uiState.value.copy(moomooAccountId = id)
    }

    fun updateMoomooMarket(market: Int) {
        _uiState.value = _uiState.value.copy(moomooMarket = market)
    }

    fun toggleMoomooWebSocket(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(moomooUseWebSocket = enabled)
    }

    // ── Common ─────────────────────────────────────────────────────────

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

    fun testConnection() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTestingConnection = true,
                apiTestMessage = "Testing connection..."
            )

            try {
                when (state.selectedBroker) {
                    BrokerType.ALPACA -> {
                        if (state.apiKey.isEmpty() || state.apiSecret.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                isTestingConnection = false,
                                apiConnected = false,
                                apiTestMessage = "Please enter API key and secret"
                            )
                            return@launch
                        }
                        val connected = brokerManager.connect()
                        _uiState.value = _uiState.value.copy(
                            isTestingConnection = false,
                            apiConnected = connected,
                            apiTestMessage = if (connected) "Alpaca connected successfully!" else "Failed to connect to Alpaca"
                        )
                    }
                    BrokerType.MOOMOO -> {
                        moomooProvider.configure(
                            accountId = state.moomooAccountId.toLongOrNull() ?: 0,
                            paperTrading = state.isPaperTrading,
                            market = state.moomooMarket
                        )
                        val connected = brokerManager.connect()
                        _uiState.value = _uiState.value.copy(
                            isTestingConnection = false,
                            apiConnected = connected,
                            apiTestMessage = if (connected) {
                                "Connected to Moomoo OpenD at ${state.moomooHost}:${state.moomooPort}!"
                            } else {
                                "Failed to connect. Ensure OpenD is running at ${state.moomooHost}:${state.moomooPort}"
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingConnection = false,
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
                val state = _uiState.value
                brokerManager.switchBroker(state.selectedBroker)

                if (state.selectedBroker == BrokerType.MOOMOO) {
                    moomooProvider.configure(
                        accountId = state.moomooAccountId.toLongOrNull() ?: 0,
                        paperTrading = state.isPaperTrading,
                        market = state.moomooMarket
                    )
                }

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
