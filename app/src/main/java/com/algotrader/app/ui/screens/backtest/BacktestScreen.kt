package com.algotrader.app.ui.screens.backtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algotrader.app.ui.components.EquityCurveChart
import com.algotrader.app.ui.components.MetricCard
import com.algotrader.app.ui.components.SectionHeader
import com.algotrader.app.ui.components.StatRow
import com.algotrader.app.ui.theme.LossRed
import com.algotrader.app.ui.theme.ProfitGreen
import com.algotrader.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestScreen(
    preselectedStrategyId: Long? = null,
    viewModel: BacktestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeframes = listOf("1Min", "5Min", "15Min", "1Hour", "1Day")
    val lookbackOptions = listOf(30, 90, 180, 365, 730)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("  Backtest", fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.strategies.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                OutlinedTextField(
                                    value = uiState.selectedStrategy?.name ?: "Select Strategy",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Strategy") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    uiState.strategies.forEach { strategy ->
                                        DropdownMenuItem(
                                            text = { Text(strategy.name) },
                                            onClick = {
                                                viewModel.selectStrategy(strategy)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = uiState.symbol,
                            onValueChange = { viewModel.updateSymbol(it) },
                            label = { Text("Symbol") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Timeframe", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(timeframes) { tf ->
                                FilterChip(
                                    selected = uiState.timeframe == tf,
                                    onClick = { viewModel.updateTimeframe(tf) },
                                    label = { Text(tf, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Lookback Period", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(lookbackOptions) { days ->
                                FilterChip(
                                    selected = uiState.lookbackDays == days,
                                    onClick = { viewModel.updateLookbackDays(days) },
                                    label = { Text("${days}d", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.stopLossPercent,
                                onValueChange = { viewModel.updateStopLoss(it) },
                                label = { Text("Stop Loss %") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.takeProfitPercent,
                                onValueChange = { viewModel.updateTakeProfit(it) },
                                label = { Text("Take Profit %") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.runBacktest() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !uiState.isRunning && uiState.selectedStrategy != null
                ) {
                    if (uiState.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(uiState.progress)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("  Run Backtest")
                    }
                }
            }

            if (uiState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            uiState.result?.let { result ->
                item {
                    SectionHeader(title = "Results")
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "Total Return",
                            value = FormatUtils.formatPercent(result.totalReturnPercent),
                            valueColor = if (result.totalReturn >= 0) ProfitGreen else LossRed,
                            subtitle = FormatUtils.formatPnl(result.totalReturn),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Sharpe Ratio",
                            value = FormatUtils.formatRatio(result.sharpeRatio),
                            valueColor = if (result.sharpeRatio >= 1) ProfitGreen else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "Max Drawdown",
                            value = FormatUtils.formatPercent(-result.maxDrawdownPercent),
                            valueColor = LossRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Win Rate",
                            value = FormatUtils.formatPercent(result.winRate),
                            valueColor = if (result.winRate >= 50) ProfitGreen else LossRed,
                            subtitle = "${result.winningTrades}W / ${result.losingTrades}L",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Equity Curve", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            EquityCurveChart(
                                data = result.equityCurve.map { it.equity },
                                modifier = Modifier.fillMaxWidth(),
                                lineColor = if (result.totalReturn >= 0) ProfitGreen else LossRed
                            )
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Detailed Statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            StatRow("Initial Capital", FormatUtils.formatCurrency(result.initialCapital))
                            StatRow("Final Capital", FormatUtils.formatCurrency(result.finalCapital),
                                valueColor = if (result.totalReturn >= 0) ProfitGreen else LossRed)
                            StatRow("Annualized Return", FormatUtils.formatPercent(result.annualizedReturn),
                                valueColor = if (result.annualizedReturn >= 0) ProfitGreen else LossRed)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            StatRow("Total Trades", "${result.totalTrades}")
                            StatRow("Profit Factor", FormatUtils.formatRatio(result.profitFactor))
                            StatRow("Sortino Ratio", FormatUtils.formatRatio(result.sortinoRatio))
                            StatRow("Average Win", FormatUtils.formatCurrency(result.averageWin), valueColor = ProfitGreen)
                            StatRow("Average Loss", FormatUtils.formatCurrency(result.averageLoss), valueColor = LossRed)
                            StatRow("Largest Win", FormatUtils.formatCurrency(result.largestWin), valueColor = ProfitGreen)
                            StatRow("Largest Loss", FormatUtils.formatCurrency(result.largestLoss), valueColor = LossRed)
                        }
                    }
                }

                if (result.trades.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Trade History (${result.trades.size})")
                    }

                    items(result.trades.take(20)) { trade ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        FormatUtils.formatDate(trade.entryTime),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${FormatUtils.formatCurrency(trade.entryPrice)} → ${FormatUtils.formatCurrency(trade.exitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        FormatUtils.formatPnl(trade.pnl),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (trade.pnl >= 0) ProfitGreen else LossRed
                                    )
                                    Text(
                                        FormatUtils.formatPercent(trade.pnlPercent),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (trade.pnlPercent >= 0) ProfitGreen else LossRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
