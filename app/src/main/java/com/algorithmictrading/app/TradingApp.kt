package com.algorithmictrading.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

enum class OrderSide {
    BUY,
    SELL,
}

enum class AppTab(val title: String) {
    Dashboard("Dashboard"),
    Strategies("Strategies"),
    Backtest("Backtest"),
    Paper("Paper Trade"),
    Risk("Risk"),
}

data class StrategyTemplate(
    val id: String,
    val name: String,
    val description: String,
    val timeframe: String,
    val defaultParams: Map<String, Double>,
    val isEnabled: Boolean = false,
)

data class MarketQuote(
    val symbol: String,
    val price: Double,
    val changePct: Double,
    val timestamp: String,
)

data class BacktestResult(
    val strategyId: String,
    val totalReturnPct: Double,
    val winRatePct: Double,
    val maxDrawdownPct: Double,
    val sharpeRatio: Double,
    val tradeCount: Int,
    val endingCapital: Double,
)

data class PaperPosition(
    val symbol: String,
    val quantity: Int,
    val avgPrice: Double,
    val marketPrice: Double,
) {
    val marketValue: Double = marketPrice * quantity
    val unrealizedPnl: Double = (marketPrice - avgPrice) * quantity
}

data class RiskConfig(
    val maxDailyLossPct: Double = 5.0,
    val maxPositionPct: Double = 20.0,
    val maxOpenPositions: Int = 5,
    val killSwitchEnabled: Boolean = false,
)

data class AppUiState(
    val strategies: List<StrategyTemplate> = emptyList(),
    val selectedStrategyId: String = "",
    val quotes: List<MarketQuote> = emptyList(),
    val positions: List<PaperPosition> = emptyList(),
    val cash: Double = STARTING_CASH,
    val equity: Double = STARTING_CASH,
    val realizedPnl: Double = 0.0,
    val riskConfig: RiskConfig = RiskConfig(),
    val backtestResult: BacktestResult? = null,
    val activityLog: List<String> = emptyList(),
    val message: String? = null,
)

private const val STARTING_CASH = 100_000.0
private val MARKET_SYMBOLS = listOf("AAPL", "MSFT", "NVDA", "AMZN", "TSLA", "SPY")

private fun defaultStrategies(): List<StrategyTemplate> =
    listOf(
        StrategyTemplate(
            id = "trend",
            name = "Trend Rider",
            description = "Momentum strategy inspired by multi-timeframe trend systems.",
            timeframe = "15m / 1h",
            defaultParams = mapOf("lookback" to 20.0, "threshold" to 0.75),
            isEnabled = true,
        ),
        StrategyTemplate(
            id = "mean_rev",
            name = "Mean Reversion",
            description = "Buy weakness and trim strength around moving average bands.",
            timeframe = "5m / 15m",
            defaultParams = mapOf("lookback" to 10.0, "zScore" to 1.8),
        ),
        StrategyTemplate(
            id = "breakout",
            name = "Volatility Breakout",
            description = "Enter range breaks with ATR-based exits and stop logic.",
            timeframe = "1h / 4h",
            defaultParams = mapOf("atrMultiplier" to 1.5, "riskUnit" to 0.5),
        ),
    )

class TradingViewModel : ViewModel() {
    private val marketDataService = MarketDataService()
    private val backtestEngine = BacktestEngine()
    private val positionBook = mutableMapOf<String, PositionBookEntry>()
    private val strategies = defaultStrategies()

    private var cashBalance = STARTING_CASH
    private var realizedPnl = 0.0

    var uiState by mutableStateOf(
        AppUiState(
            strategies = strategies,
            selectedStrategyId = strategies.first().id,
        ),
    )
        private set

    init {
        refreshQuotes()
        log("Session started in paper trading mode.")
    }

    fun refreshQuotes() {
        val latest = marketDataService.refreshQuotes(MARKET_SYMBOLS)
        publishState(quotes = latest)
        log("Market snapshot refreshed for ${latest.size} symbols.")
    }

    fun selectStrategy(strategyId: String) {
        publishState(selectedStrategyId = strategyId)
    }

    fun setStrategyEnabled(strategyId: String, isEnabled: Boolean) {
        val updated = uiState.strategies.map { strategy ->
            if (strategy.id == strategyId) strategy.copy(isEnabled = isEnabled) else strategy
        }
        publishState(strategies = updated)
        log(
            if (isEnabled) {
                "Enabled strategy: $strategyId"
            } else {
                "Disabled strategy: $strategyId"
            },
        )
    }

    fun runBacktest(lookbackDays: Int, startingCapital: Double) {
        val selected = uiState.strategies.first { it.id == uiState.selectedStrategyId }
        val result = backtestEngine.run(
            strategy = selected,
            lookbackDays = lookbackDays,
            startingCapital = startingCapital,
            riskConfig = uiState.riskConfig,
        )
        publishState(backtestResult = result, message = "Backtest complete for ${selected.name}.")
        log(
            "Backtest ${selected.id}: return ${result.totalReturnPct.percent()} | " +
                "maxDD ${result.maxDrawdownPct.percent()}",
        )
    }

    fun placeOrder(symbolInput: String, quantityInput: String, side: OrderSide) {
        val symbol = symbolInput.trim().uppercase(Locale.US)
        val quantity = quantityInput.trim().toIntOrNull()

        if (symbol.isEmpty()) {
            publishState(message = "Symbol is required.")
            return
        }
        if (quantity == null || quantity <= 0) {
            publishState(message = "Quantity must be a positive integer.")
            return
        }

        val quote = uiState.quotes.firstOrNull { it.symbol == symbol }
        if (quote == null) {
            publishState(message = "No live quote for $symbol. Refresh the market feed first.")
            return
        }

        val validationError = validateOrder(symbol, quantity, side, quote.price)
        if (validationError != null) {
            publishState(message = validationError)
            return
        }

        if (side == OrderSide.BUY) {
            executeBuy(symbol, quantity, quote.price)
        } else {
            executeSell(symbol, quantity, quote.price)
        }

        val action = "${side.name} $quantity $symbol @ ${quote.price.money()}"
        publishState(message = "Executed: $action")
        log(action)
    }

    fun updateRiskConfig(config: RiskConfig) {
        publishState(riskConfig = config)
        log(
            "Risk updated: dailyLoss=${config.maxDailyLossPct.percent()} " +
                "maxPos=${config.maxPositionPct.percent()} open=${config.maxOpenPositions}",
        )
    }

    private fun validateOrder(symbol: String, quantity: Int, side: OrderSide, price: Double): String? {
        val risk = uiState.riskConfig
        if (risk.killSwitchEnabled) {
            return "Kill switch is enabled. Disable it before submitting orders."
        }
        if (realizedPnl <= -STARTING_CASH * (risk.maxDailyLossPct / 100.0)) {
            return "Daily loss limit reached for this paper account."
        }

        val orderValue = quantity * price
        val equity = currentEquity(uiState.quotes)
        val positionCap = equity * (risk.maxPositionPct / 100.0)
        val existing = positionBook[symbol]

        if (side == OrderSide.BUY) {
            if (orderValue > cashBalance) {
                return "Not enough cash for this order."
            }
            if (existing == null && positionBook.size >= risk.maxOpenPositions) {
                return "Open position limit reached (${risk.maxOpenPositions})."
            }

            val currentExposure = (existing?.quantity ?: 0) * price
            if (currentExposure + orderValue > positionCap) {
                return "Position cap exceeded for $symbol (limit ${positionCap.money()})."
            }
        } else {
            if (existing == null || existing.quantity < quantity) {
                return "Not enough $symbol shares to sell."
            }
        }
        return null
    }

    private fun executeBuy(symbol: String, quantity: Int, price: Double) {
        val existing = positionBook[symbol]
        if (existing == null) {
            positionBook[symbol] = PositionBookEntry(quantity = quantity, avgPrice = price)
        } else {
            val totalQty = existing.quantity + quantity
            val blended = ((existing.quantity * existing.avgPrice) + (quantity * price)) / totalQty
            existing.quantity = totalQty
            existing.avgPrice = blended
        }
        cashBalance -= quantity * price
        publishState()
    }

    private fun executeSell(symbol: String, quantity: Int, price: Double) {
        val existing = positionBook.getValue(symbol)
        realizedPnl += (price - existing.avgPrice) * quantity
        existing.quantity -= quantity
        if (existing.quantity <= 0) {
            positionBook.remove(symbol)
        }
        cashBalance += quantity * price
        publishState()
    }

    private fun publishState(
        strategies: List<StrategyTemplate> = uiState.strategies,
        selectedStrategyId: String = uiState.selectedStrategyId,
        quotes: List<MarketQuote> = uiState.quotes,
        riskConfig: RiskConfig = uiState.riskConfig,
        backtestResult: BacktestResult? = uiState.backtestResult,
        message: String? = uiState.message,
    ) {
        uiState = uiState.copy(
            strategies = strategies,
            selectedStrategyId = selectedStrategyId,
            quotes = quotes,
            positions = buildPositions(quotes),
            cash = cashBalance,
            equity = currentEquity(quotes),
            realizedPnl = realizedPnl,
            riskConfig = riskConfig,
            backtestResult = backtestResult,
            message = message,
        )
    }

    private fun buildPositions(quotes: List<MarketQuote>): List<PaperPosition> {
        val quoteMap = quotes.associateBy { it.symbol }
        return positionBook.entries
            .sortedBy { it.key }
            .map { (symbol, entry) ->
                val mark = quoteMap[symbol]?.price ?: entry.avgPrice
                PaperPosition(
                    symbol = symbol,
                    quantity = entry.quantity,
                    avgPrice = entry.avgPrice,
                    marketPrice = mark,
                )
            }
    }

    private fun currentEquity(quotes: List<MarketQuote>): Double {
        val quoteMap = quotes.associateBy { it.symbol }
        val marketValue = positionBook.entries.sumOf { (symbol, entry) ->
            val mark = quoteMap[symbol]?.price ?: entry.avgPrice
            mark * entry.quantity
        }
        return cashBalance + marketValue
    }

    private fun log(entry: String) {
        val line = "${LocalTime.now().format(TIME_FORMATTER)} - $entry"
        uiState = uiState.copy(activityLog = (listOf(line) + uiState.activityLog).take(25))
    }

    private data class PositionBookEntry(
        var quantity: Int,
        var avgPrice: Double,
    )

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}

class MarketDataService {
    private val random = Random(17)
    private val prices = mutableMapOf(
        "AAPL" to 211.0,
        "MSFT" to 497.0,
        "NVDA" to 128.0,
        "AMZN" to 201.0,
        "TSLA" to 227.0,
        "SPY" to 612.0,
    )

    fun refreshQuotes(symbols: List<String>): List<MarketQuote> {
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        return symbols.map { symbol ->
            val previous = prices[symbol] ?: 100.0
            val changePct = random.nextDouble(-2.2, 2.2)
            val updated = max(1.0, previous * (1.0 + (changePct / 100.0)))
            prices[symbol] = updated
            MarketQuote(
                symbol = symbol,
                price = updated,
                changePct = changePct,
                timestamp = now,
            )
        }
    }
}

class BacktestEngine {
    fun run(
        strategy: StrategyTemplate,
        lookbackDays: Int,
        startingCapital: Double,
        riskConfig: RiskConfig,
    ): BacktestResult {
        val safeDays = lookbackDays.coerceIn(30, 1460)
        val riskWeight = (riskConfig.maxPositionPct / 100.0).coerceIn(0.05, 0.35)

        val expectedEdge = when (strategy.id) {
            "trend" -> 0.0009
            "mean_rev" -> 0.0006
            "breakout" -> 0.0008
            else -> 0.0004
        }

        val random = Random(strategy.id.hashCode() + safeDays)
        var equity = startingCapital
        var peak = startingCapital
        var maxDrawdown = 0.0
        var wins = 0
        val dailyReturns = mutableListOf<Double>()

        repeat(safeDays) {
            val noise = random.nextDouble(-0.012, 0.012)
            val regime = random.nextDouble(-0.004, 0.004)
            val rawReturn = (expectedEdge + noise + regime) * (riskWeight / 0.2)
            val bounded = rawReturn.coerceIn(-0.05, 0.05)
            equity *= (1.0 + bounded)
            dailyReturns += bounded
            if (bounded > 0) wins++
            if (equity > peak) peak = equity
            val drawdown = (peak - equity) / peak
            if (drawdown > maxDrawdown) maxDrawdown = drawdown
        }

        val mean = dailyReturns.average()
        val variance = dailyReturns.sumOf { (it - mean) * (it - mean) } / dailyReturns.size
        val stdDev = sqrt(variance)
        val sharpe = if (stdDev < 0.00001) 0.0 else (mean / stdDev) * sqrt(252.0)
        val totalReturn = ((equity / startingCapital) - 1.0) * 100.0
        val winRate = (wins.toDouble() / safeDays.toDouble()) * 100.0

        return BacktestResult(
            strategyId = strategy.id,
            totalReturnPct = totalReturn,
            winRatePct = winRate,
            maxDrawdownPct = maxDrawdown * 100.0,
            sharpeRatio = sharpe,
            tradeCount = safeDays,
            endingCapital = equity,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingApp(viewModel: TradingViewModel = viewModel()) {
    val state = viewModel.uiState
    var tab by rememberSaveable { mutableStateOf(AppTab.Dashboard.name) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Algo Trader Android") },
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item.name,
                        onClick = { tab = item.name },
                        icon = { Text(item.title.take(1)) },
                        label = { Text(item.title) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            state.message?.let { message ->
                MessageBanner(message = message)
                Spacer(modifier = Modifier.height(10.dp))
            }

            when (AppTab.valueOf(tab)) {
                AppTab.Dashboard -> DashboardScreen(
                    state = state,
                    onRefresh = viewModel::refreshQuotes,
                )

                AppTab.Strategies -> StrategiesScreen(
                    state = state,
                    onStrategySelected = viewModel::selectStrategy,
                    onStrategyToggled = viewModel::setStrategyEnabled,
                )

                AppTab.Backtest -> BacktestScreen(
                    state = state,
                    onRunBacktest = viewModel::runBacktest,
                )

                AppTab.Paper -> PaperTradeScreen(
                    state = state,
                    onOrder = viewModel::placeOrder,
                )

                AppTab.Risk -> RiskScreen(
                    state = state,
                    onRiskChanged = viewModel::updateRiskConfig,
                )
            }
        }
    }
}

@Composable
private fun MessageBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDDF4FF)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DashboardScreen(state: AppUiState, onRefresh: () -> Unit) {
    Text("Paper Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Equity: ${state.equity.money()}", fontWeight = FontWeight.SemiBold)
            Text("Cash: ${state.cash.money()}")
            Text("Realized PnL: ${state.realizedPnl.money()}")
            Text("Open Positions: ${state.positions.size}")
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Live Market Snapshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Button(onClick = onRefresh) { Text("Refresh") }
    }

    Spacer(modifier = Modifier.height(8.dp))
    state.quotes.forEach { quote ->
        QuoteRow(quote = quote)
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    if (state.activityLog.isEmpty()) {
        Text("No activity yet.")
    } else {
        state.activityLog.take(8).forEach { item ->
            Text(item, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

@Composable
private fun QuoteRow(quote: MarketQuote) {
    val accent = if (quote.changePct >= 0.0) Color(0xFF0F9D58) else Color(0xFFB3261E)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(quote.symbol, fontWeight = FontWeight.SemiBold)
                Text("Updated ${quote.timestamp}", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(quote.price.money(), fontWeight = FontWeight.Bold)
                Text(
                    text = quote.changePct.percent(),
                    color = accent,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StrategiesScreen(
    state: AppUiState,
    onStrategySelected: (String) -> Unit,
    onStrategyToggled: (String, Boolean) -> Unit,
) {
    Text("Strategy Lab", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "Inspired by Lean/Freqtrade/Hummingbot plugin-style strategy management.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(modifier = Modifier.height(12.dp))

    state.strategies.forEach { strategy ->
        val selected = strategy.id == state.selectedStrategyId
        val borderColor = if (selected) Color(0xFF90CAF9) else Color(0xFFE0E0E0)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(borderColor.copy(alpha = 0.15f))
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strategy.name, fontWeight = FontWeight.SemiBold)
                        Text(strategy.timeframe, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = strategy.isEnabled,
                        onCheckedChange = { enabled -> onStrategyToggled(strategy.id, enabled) },
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(strategy.description, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    strategy.defaultParams.forEach { (param, value) ->
                        Text("$param=${value.trimmed()}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onStrategySelected(strategy.id) }) {
                    Text(if (selected) "Selected" else "Select Strategy")
                }
            }
        }
    }
}

@Composable
private fun BacktestScreen(
    state: AppUiState,
    onRunBacktest: (lookbackDays: Int, startingCapital: Double) -> Unit,
) {
    var lookbackInput by rememberSaveable { mutableStateOf("180") }
    var capitalInput by rememberSaveable { mutableStateOf("10000") }

    Text("Backtest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "Run quick simulation before enabling live broker integrations.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
        value = lookbackInput,
        onValueChange = { lookbackInput = it },
        label = { Text("Lookback days") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = capitalInput,
        onValueChange = { capitalInput = it },
        label = { Text("Starting capital (USD)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(10.dp))

    Button(
        onClick = {
            val days = lookbackInput.toIntOrNull() ?: 180
            val capital = capitalInput.toDoubleOrNull() ?: 10_000.0
            onRunBacktest(days, capital)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Run Backtest")
    }

    Spacer(modifier = Modifier.height(12.dp))
    val result = state.backtestResult
    if (result == null) {
        Text("No backtest run yet.")
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Strategy: ${result.strategyId}", fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                MetricRow("Total Return", result.totalReturnPct.percent())
                MetricRow("Win Rate", result.winRatePct.percent())
                MetricRow("Max Drawdown", result.maxDrawdownPct.percent())
                MetricRow("Sharpe Ratio", result.sharpeRatio.trimmed())
                MetricRow("Trades", result.tradeCount.toString())
                MetricRow("Ending Capital", result.endingCapital.money())
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PaperTradeScreen(
    state: AppUiState,
    onOrder: (symbolInput: String, quantityInput: String, side: OrderSide) -> Unit,
) {
    var symbolInput by rememberSaveable { mutableStateOf("AAPL") }
    var quantityInput by rememberSaveable { mutableStateOf("10") }

    Text("Paper Trading", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "Dry-run execution flow inspired by paperMoney and dry-run modes in leading platforms.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
        value = symbolInput,
        onValueChange = { symbolInput = it },
        label = { Text("Symbol") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = quantityInput,
        onValueChange = { quantityInput = it },
        label = { Text("Quantity") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { onOrder(symbolInput, quantityInput, OrderSide.BUY) },
            modifier = Modifier.weight(1f),
        ) {
            Text("Buy")
        }
        Button(
            onClick = { onOrder(symbolInput, quantityInput, OrderSide.SELL) },
            modifier = Modifier.weight(1f),
        ) {
            Text("Sell")
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text("Open Positions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    if (state.positions.isEmpty()) {
        Text("No open positions.")
    } else {
        state.positions.forEach { position ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${position.symbol} x${position.quantity}", fontWeight = FontWeight.SemiBold)
                        Text(position.marketValue.money())
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("Avg: ${position.avgPrice.money()}  Mark: ${position.marketPrice.money()}")
                    Text(
                        "Unrealized: ${position.unrealizedPnl.money()}",
                        color = if (position.unrealizedPnl >= 0.0) Color(0xFF0F9D58) else Color(0xFFB3261E),
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskScreen(
    state: AppUiState,
    onRiskChanged: (RiskConfig) -> Unit,
) {
    val risk = state.riskConfig

    Text("Risk Controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "Borrowed from institutional frameworks: max daily loss, position sizing, and kill switch.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(modifier = Modifier.height(12.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Max Daily Loss: ${risk.maxDailyLossPct.percent()}")
            Slider(
                value = risk.maxDailyLossPct.toFloat(),
                valueRange = 1f..20f,
                onValueChange = { onRiskChanged(risk.copy(maxDailyLossPct = it.toDouble())) },
            )

            Text("Max Position Size: ${risk.maxPositionPct.percent()} of equity")
            Slider(
                value = risk.maxPositionPct.toFloat(),
                valueRange = 2f..50f,
                onValueChange = { onRiskChanged(risk.copy(maxPositionPct = it.toDouble())) },
            )

            val maxOpenFloat = risk.maxOpenPositions.toFloat()
            Text("Max Open Positions: ${risk.maxOpenPositions}")
            Slider(
                value = maxOpenFloat,
                valueRange = 1f..20f,
                steps = 18,
                onValueChange = {
                    onRiskChanged(risk.copy(maxOpenPositions = it.roundToInt()))
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Kill Switch")
                Switch(
                    checked = risk.killSwitchEnabled,
                    onCheckedChange = { enabled ->
                        onRiskChanged(risk.copy(killSwitchEnabled = enabled))
                    },
                )
            }
        }
    }
}

private fun Double.money(): String = NumberFormat.getCurrencyInstance(Locale.US).format(this)

private fun Double.percent(): String = "${trimmed()}%"

private fun Double.trimmed(): String = String.format(Locale.US, "%.2f", this)
