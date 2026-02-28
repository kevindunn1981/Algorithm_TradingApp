package com.algotrader.app.ui.screens.backtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.model.BacktestResult
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.data.model.Strategy
import com.algotrader.app.data.repository.MarketDataRepository
import com.algotrader.app.data.repository.StrategyRepository
import com.algotrader.app.engine.BacktestEngine
import com.algotrader.app.engine.strategy.templates.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class BacktestUiState(
    val strategies: List<Strategy> = emptyList(),
    val selectedStrategy: Strategy? = null,
    val symbol: String = "AAPL",
    val timeframe: String = "1Day",
    val lookbackDays: Int = 365,
    val initialCapital: Double = 100_000.0,
    val positionSizePercent: Double = 100.0,
    val stopLossPercent: String = "",
    val takeProfitPercent: String = "",
    val isRunning: Boolean = false,
    val result: BacktestResult? = null,
    val error: String? = null,
    val progress: String = ""
)

@HiltViewModel
class BacktestViewModel @Inject constructor(
    private val strategyRepository: StrategyRepository,
    private val marketDataRepository: MarketDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BacktestUiState())
    val uiState: StateFlow<BacktestUiState> = _uiState.asStateFlow()

    private val backtestEngine = BacktestEngine()

    init {
        loadStrategies()
    }

    private fun loadStrategies() {
        viewModelScope.launch {
            val strategies = strategyRepository.getAllStrategies().first()
            _uiState.value = _uiState.value.copy(
                strategies = strategies,
                selectedStrategy = strategies.firstOrNull()
            )
        }
    }

    fun selectStrategy(strategy: Strategy) {
        _uiState.value = _uiState.value.copy(selectedStrategy = strategy)
    }

    fun updateSymbol(symbol: String) {
        _uiState.value = _uiState.value.copy(symbol = symbol.uppercase())
    }

    fun updateTimeframe(timeframe: String) {
        _uiState.value = _uiState.value.copy(timeframe = timeframe)
    }

    fun updateLookbackDays(days: Int) {
        _uiState.value = _uiState.value.copy(lookbackDays = days)
    }

    fun updateInitialCapital(capital: Double) {
        _uiState.value = _uiState.value.copy(initialCapital = capital)
    }

    fun updatePositionSize(percent: Double) {
        _uiState.value = _uiState.value.copy(positionSizePercent = percent)
    }

    fun updateStopLoss(value: String) {
        _uiState.value = _uiState.value.copy(stopLossPercent = value)
    }

    fun updateTakeProfit(value: String) {
        _uiState.value = _uiState.value.copy(takeProfitPercent = value)
    }

    fun runBacktest() {
        val state = _uiState.value
        val selectedStrategy = state.selectedStrategy ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, error = null, progress = "Loading market data...")

            try {
                val endDate = Instant.now()
                val startDate = endDate.minus(state.lookbackDays.toLong(), ChronoUnit.DAYS)

                val bars = marketDataRepository.getBars(
                    symbol = state.symbol,
                    timeframe = state.timeframe,
                    start = startDate.toString(),
                    end = endDate.toString()
                )

                if (bars.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isRunning = false,
                        error = "No market data available. Using sample data for demonstration."
                    )
                    runWithSampleData(selectedStrategy, state)
                    return@launch
                }

                _uiState.value = _uiState.value.copy(progress = "Running backtest...")

                val tradingStrategy = resolveStrategy(selectedStrategy)
                val config = BacktestEngine.BacktestConfig(
                    initialCapital = state.initialCapital,
                    positionSizePercent = state.positionSizePercent,
                    stopLossPercent = state.stopLossPercent.toDoubleOrNull(),
                    takeProfitPercent = state.takeProfitPercent.toDoubleOrNull(),
                    symbol = state.symbol
                )

                val result = backtestEngine.run(tradingStrategy, bars, config)

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    result = result.copy(strategyId = selectedStrategy.id),
                    progress = ""
                )
            } catch (e: Exception) {
                runWithSampleData(selectedStrategy, state)
            }
        }
    }

    private suspend fun runWithSampleData(selectedStrategy: Strategy, state: BacktestUiState) {
        try {
            val sampleBars = generateSampleData(state.symbol, state.lookbackDays)
            val tradingStrategy = resolveStrategy(selectedStrategy)
            val config = BacktestEngine.BacktestConfig(
                initialCapital = state.initialCapital,
                positionSizePercent = state.positionSizePercent,
                stopLossPercent = state.stopLossPercent.toDoubleOrNull(),
                takeProfitPercent = state.takeProfitPercent.toDoubleOrNull(),
                symbol = state.symbol
            )

            val result = backtestEngine.run(tradingStrategy, sampleBars, config)
            _uiState.value = _uiState.value.copy(
                isRunning = false,
                result = result.copy(strategyId = selectedStrategy.id),
                progress = "",
                error = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isRunning = false,
                error = "Backtest failed: ${e.message}",
                progress = ""
            )
        }
    }

    private fun resolveStrategy(strategy: Strategy): com.algotrader.app.engine.strategy.TradingStrategy {
        val name = strategy.name.lowercase()
        val code = strategy.code.lowercase()

        return when {
            name.contains("turtle") || code.contains("turtle") -> TurtleTradingStrategy()
            name.contains("ichimoku") || code.contains("ichimoku") -> IchimokuStrategy()
            name.contains("vwap") || code.contains("vwap mean") -> VwapMeanReversionStrategy()
            name.contains("keltner") || code.contains("keltner") -> KeltnerChannelStrategy()
            name.contains("adx") || code.contains("adx trend") -> AdxTrendStrategy()
            name.contains("z-score") || code.contains("z-score") -> PairsTradingStrategy()
            name.contains("opening range") || code.contains("opening range") -> OpeningRangeBreakoutStrategy()
            name.contains("supertrend") || code.contains("supertrend") -> SupertrendStrategy()
            name.contains("triple ema") || code.contains("triple ema") -> TripleEmaCrossoverStrategy()
            name.contains("confluence") || code.contains("confluence") -> MeanReversionConfluenceStrategy()
            name.contains("breakout momentum") || code.contains("breakout momentum") -> BreakoutMomentumStrategy()
            name.contains("williams") || code.contains("williams") -> WilliamsRStrategy()
            name.contains("cci") || code.contains("cci") -> CciStrategy()
            name.contains("dual ma") || code.contains("dual ma") -> DualMaVolumeStrategy()
            name.contains("elder") || code.contains("elder") -> ElderTripleScreenStrategy()
            name.contains("donchian") || code.contains("donchian") -> DonchianBreakoutStrategy()
            name.contains("stochastic") || code.contains("stochastic") -> StochasticMomentumStrategy()
            name.contains("sma") || code.contains("sma crossover") -> SmaCrossoverStrategy()
            name.contains("rsi") || code.contains("rsi") -> RsiStrategy()
            name.contains("macd") || code.contains("macd") -> MacdStrategy()
            name.contains("bollinger") || code.contains("bollinger") -> BollingerBandStrategy()
            name.contains("momentum") || code.contains("momentum") -> MomentumStrategy()
            else -> SmaCrossoverStrategy()
        }
    }

    private fun generateSampleData(symbol: String, days: Int): List<PriceBar> {
        val bars = mutableListOf<PriceBar>()
        var price = 150.0
        val random = java.util.Random(42)
        val start = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)

        for (i in 0 until days) {
            val change = (random.nextGaussian() * 2.0)
            price *= (1 + change / 100.0)
            price = price.coerceAtLeast(10.0)

            val high = price * (1 + random.nextDouble() * 0.02)
            val low = price * (1 - random.nextDouble() * 0.02)
            val open = price + (random.nextGaussian() * 0.5)
            val volume = (1_000_000 + random.nextInt(5_000_000)).toLong()

            bars.add(
                PriceBar(
                    symbol = symbol,
                    timestamp = start.plus(i.toLong(), ChronoUnit.DAYS),
                    open = open,
                    high = high,
                    low = low,
                    close = price,
                    volume = volume
                )
            )
        }
        return bars
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null, error = null)
    }
}
