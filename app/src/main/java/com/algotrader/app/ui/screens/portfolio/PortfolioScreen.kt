package com.algotrader.app.ui.screens.portfolio

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.algotrader.app.ui.components.EmptyStateMessage
import com.algotrader.app.ui.components.LoadingScreen
import com.algotrader.app.ui.components.MetricCard
import com.algotrader.app.ui.components.PnlDisplay
import com.algotrader.app.ui.components.StatRow
import com.algotrader.app.ui.theme.LossRed
import com.algotrader.app.ui.theme.ProfitGreen
import com.algotrader.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Positions", "Orders", "Trades")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("  Portfolio", fontWeight = FontWeight.Bold)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        if (uiState.isLoading) {
            LoadingScreen()
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Account Summary",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.account?.let { account ->
                            StatRow("Equity", FormatUtils.formatCurrency(account.equity))
                            StatRow("Cash", FormatUtils.formatCurrency(account.cash))
                            StatRow("Buying Power", FormatUtils.formatCurrency(account.buyingPower))
                            StatRow("Portfolio Value", FormatUtils.formatCurrency(account.portfolioValue))
                        } ?: run {
                            Text(
                                "Connect your broker in Settings to view account data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Unrealized P&L",
                        value = FormatUtils.formatPnl(uiState.totalUnrealizedPnl),
                        valueColor = if (uiState.totalUnrealizedPnl >= 0) ProfitGreen else LossRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Positions",
                        value = "${uiState.positions.size}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(title) }
                        )
                    }
                }
            }

            when (uiState.selectedTab) {
                0 -> {
                    if (uiState.positions.isEmpty()) {
                        item { EmptyStateMessage("No open positions") }
                    } else {
                        items(uiState.positions) { position ->
                            Card(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            position.symbol,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${FormatUtils.formatNumber(position.quantity)} @ ${FormatUtils.formatCurrency(position.averageEntryPrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Value: ${FormatUtils.formatCurrency(position.marketValue)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            FormatUtils.formatCurrency(position.currentPrice),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        PnlDisplay(value = position.unrealizedPnl)
                                        Text(
                                            FormatUtils.formatPercent(position.unrealizedPnlPercent),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (position.unrealizedPnl >= 0) ProfitGreen else LossRed
                                        )
                                        TextButton(onClick = { viewModel.closePosition(position.symbol) }) {
                                            Text("Close", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (uiState.orders.isEmpty()) {
                        item { EmptyStateMessage("No recent orders") }
                    } else {
                        items(uiState.orders) { order ->
                            Card(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "${order.side.name} ${order.symbol}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${order.type.name} - ${FormatUtils.formatNumber(order.quantity)} shares",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            FormatUtils.formatDateTime(order.submittedAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            order.status.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (order.status.name) {
                                                "FILLED" -> ProfitGreen
                                                "CANCELED", "REJECTED" -> LossRed
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        if (order.filledPrice != null) {
                                            Text(
                                                "@ ${FormatUtils.formatCurrency(order.filledPrice)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (uiState.recentTrades.isEmpty()) {
                        item { EmptyStateMessage("No trades recorded yet") }
                    } else {
                        items(uiState.recentTrades) { trade ->
                            Card(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                                            "${trade.side} ${trade.symbol}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${FormatUtils.formatNumber(trade.quantity)} @ ${FormatUtils.formatCurrency(trade.price)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    PnlDisplay(value = trade.pnl)
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
