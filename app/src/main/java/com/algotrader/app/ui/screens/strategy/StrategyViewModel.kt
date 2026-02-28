package com.algotrader.app.ui.screens.strategy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.model.Strategy
import com.algotrader.app.data.model.StrategyLanguage
import com.algotrader.app.data.repository.StrategyRepository
import com.algotrader.app.engine.strategy.templates.AdxTrendStrategy
import com.algotrader.app.engine.strategy.templates.BollingerBandStrategy
import com.algotrader.app.engine.strategy.templates.BreakoutMomentumStrategy
import com.algotrader.app.engine.strategy.templates.CciStrategy
import com.algotrader.app.engine.strategy.templates.DonchianBreakoutStrategy
import com.algotrader.app.engine.strategy.templates.DualMaVolumeStrategy
import com.algotrader.app.engine.strategy.templates.ElderTripleScreenStrategy
import com.algotrader.app.engine.strategy.templates.IchimokuStrategy
import com.algotrader.app.engine.strategy.templates.KeltnerChannelStrategy
import com.algotrader.app.engine.strategy.templates.MacdStrategy
import com.algotrader.app.engine.strategy.templates.MeanReversionConfluenceStrategy
import com.algotrader.app.engine.strategy.templates.MomentumStrategy
import com.algotrader.app.engine.strategy.templates.OpeningRangeBreakoutStrategy
import com.algotrader.app.engine.strategy.templates.PairsTradingStrategy
import com.algotrader.app.engine.strategy.templates.RsiStrategy
import com.algotrader.app.engine.strategy.templates.SmaCrossoverStrategy
import com.algotrader.app.engine.strategy.templates.StochasticMomentumStrategy
import com.algotrader.app.engine.strategy.templates.SupertrendStrategy
import com.algotrader.app.engine.strategy.templates.TripleEmaCrossoverStrategy
import com.algotrader.app.engine.strategy.templates.TurtleTradingStrategy
import com.algotrader.app.engine.strategy.templates.VwapMeanReversionStrategy
import com.algotrader.app.engine.strategy.templates.WilliamsRStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StrategyListUiState(
    val strategies: List<Strategy> = emptyList(),
    val isLoading: Boolean = true
)

data class StrategyEditorUiState(
    val strategy: Strategy? = null,
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

data class StrategyTemplate(
    val name: String,
    val description: String,
    val code: String
)

@HiltViewModel
class StrategyViewModel @Inject constructor(
    private val strategyRepository: StrategyRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(StrategyListUiState())
    val listState: StateFlow<StrategyListUiState> = _listState.asStateFlow()

    private val _editorState = MutableStateFlow(StrategyEditorUiState())
    val editorState: StateFlow<StrategyEditorUiState> = _editorState.asStateFlow()

    val templates = listOf(
        // Trend Following
        StrategyTemplate("SMA Crossover", "Golden/Death cross strategy", SmaCrossoverStrategy.TEMPLATE_CODE),
        StrategyTemplate("MACD Crossover", "Signal line crossover trades", MacdStrategy.TEMPLATE_CODE),
        StrategyTemplate("Supertrend", "ATR-based trend with dynamic support/resistance", SupertrendStrategy.TEMPLATE_CODE),
        StrategyTemplate("ADX Trend Following", "DI+/DI- crossover with ADX strength filter", AdxTrendStrategy.TEMPLATE_CODE),
        StrategyTemplate("Turtle Trading", "Donchian Channel breakout (Richard Dennis)", TurtleTradingStrategy.TEMPLATE_CODE),
        StrategyTemplate("Donchian Breakout", "Channel breakout with EMA trend filter", DonchianBreakoutStrategy.TEMPLATE_CODE),
        StrategyTemplate("Ichimoku Cloud", "Tenkan/Kijun cross filtered by Kumo cloud", IchimokuStrategy.TEMPLATE_CODE),
        StrategyTemplate("Triple EMA Crossover", "3 EMA ribbon + RSI + volume confirmation", TripleEmaCrossoverStrategy.TEMPLATE_CODE),
        // Mean Reversion
        StrategyTemplate("RSI Mean Reversion", "Oversold/Overbought reversals", RsiStrategy.TEMPLATE_CODE),
        StrategyTemplate("Bollinger Bands", "Mean reversion at Bollinger bands", BollingerBandStrategy.TEMPLATE_CODE),
        StrategyTemplate("Keltner Channel", "EMA + ATR band mean reversion with RSI", KeltnerChannelStrategy.TEMPLATE_CODE),
        StrategyTemplate("VWAP Mean Reversion", "Fade deviations from volume-weighted avg price", VwapMeanReversionStrategy.TEMPLATE_CODE),
        StrategyTemplate("Z-Score Mean Reversion", "Statistical Z-score deviation trading", PairsTradingStrategy.TEMPLATE_CODE),
        StrategyTemplate("CCI Trend & Reversal", "Commodity Channel Index signals with trend", CciStrategy.TEMPLATE_CODE),
        StrategyTemplate("Williams %R", "Percent Range reversals with EMA trend filter", WilliamsRStrategy.TEMPLATE_CODE),
        // Multi-Indicator / Advanced
        StrategyTemplate("Multi-Indicator Momentum", "EMA + RSI combined", MomentumStrategy.TEMPLATE_CODE),
        StrategyTemplate("Mean Reversion Confluence", "RSI + Bollinger + MACD for reversals", MeanReversionConfluenceStrategy.TEMPLATE_CODE),
        StrategyTemplate("Elder Triple Screen", "Multi-timeframe MACD + Stochastic + price", ElderTripleScreenStrategy.TEMPLATE_CODE),
        StrategyTemplate("Stochastic Momentum", "K/D cross at extremes + 200 EMA trend", StochasticMomentumStrategy.TEMPLATE_CODE),
        // Breakout / Momentum
        StrategyTemplate("Opening Range Breakout", "N-bar range breakout with volume confirmation", OpeningRangeBreakoutStrategy.TEMPLATE_CODE),
        StrategyTemplate("Breakout Momentum", "Volume-confirmed breakout + EMA trend", BreakoutMomentumStrategy.TEMPLATE_CODE),
        StrategyTemplate("Dual MA + Volume", "EMA crossover with above-avg volume filter", DualMaVolumeStrategy.TEMPLATE_CODE)
    )

    init {
        loadStrategies()
    }

    fun loadStrategies() {
        viewModelScope.launch {
            strategyRepository.getAllStrategies().collect { strategies ->
                _listState.value = StrategyListUiState(
                    strategies = strategies,
                    isLoading = false
                )
            }
        }
    }

    fun loadStrategy(id: Long) {
        viewModelScope.launch {
            val strategy = strategyRepository.getStrategyById(id)
            if (strategy != null) {
                _editorState.value = StrategyEditorUiState(
                    strategy = strategy,
                    code = strategy.code,
                    name = strategy.name,
                    description = strategy.description,
                    isNew = false
                )
            }
        }
    }

    fun newStrategy() {
        _editorState.value = StrategyEditorUiState(
            code = SmaCrossoverStrategy.TEMPLATE_CODE,
            name = "New Strategy",
            description = "Describe your strategy",
            isNew = true
        )
    }

    fun loadTemplate(template: StrategyTemplate) {
        _editorState.value = _editorState.value.copy(
            code = template.code,
            name = template.name,
            description = template.description
        )
    }

    fun updateCode(code: String) {
        _editorState.value = _editorState.value.copy(code = code)
    }

    fun updateName(name: String) {
        _editorState.value = _editorState.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _editorState.value = _editorState.value.copy(description = description)
    }

    fun saveStrategy() {
        viewModelScope.launch {
            _editorState.value = _editorState.value.copy(isSaving = true)
            try {
                val state = _editorState.value
                val strategy = Strategy(
                    id = state.strategy?.id ?: 0,
                    name = state.name,
                    description = state.description,
                    code = state.code,
                    language = StrategyLanguage.KOTLIN_DSL
                )
                if (state.isNew) {
                    strategyRepository.saveStrategy(strategy)
                } else {
                    strategyRepository.updateStrategy(strategy)
                }
                _editorState.value = _editorState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )
            } catch (e: Exception) {
                _editorState.value = _editorState.value.copy(
                    isSaving = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteStrategy(strategy: Strategy) {
        viewModelScope.launch {
            strategyRepository.deleteStrategy(strategy)
        }
    }

    fun toggleActive(strategy: Strategy) {
        viewModelScope.launch {
            strategyRepository.setActive(strategy.id, !strategy.isActive)
        }
    }
}
