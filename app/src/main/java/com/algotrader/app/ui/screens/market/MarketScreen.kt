package com.algotrader.app.ui.screens.market

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.algotrader.app.ui.components.CandlestickChart
import com.algotrader.app.ui.components.MetricCard
import com.algotrader.app.ui.components.PercentageChange
import com.algotrader.app.ui.components.SectionHeader
import com.algotrader.app.ui.components.StockTickerCard
import com.algotrader.app.ui.theme.LossRed
import com.algotrader.app.ui.theme.ProfitGreen
import com.algotrader.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    viewModel: MarketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("  Market", fontWeight = FontWeight.Bold)
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("Search symbol (e.g., AAPL)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (uiState.searchQuery.isNotEmpty()) {
                item {
                    Card(
                        onClick = {
                            val symbol = uiState.searchQuery.uppercase()
                            viewModel.addToWatchlist(symbol)
                            viewModel.selectSymbol(symbol)
                            viewModel.updateSearchQuery("")
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Add ${uiState.searchQuery.uppercase()} to watchlist",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Popular Stocks")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.popularSymbols) { symbol ->
                        AssistChip(
                            onClick = {
                                viewModel.selectSymbol(symbol)
                                viewModel.addToWatchlist(symbol)
                            },
                            label = { Text(symbol, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            if (uiState.selectedQuote != null) {
                item {
                    val quote = uiState.selectedQuote!!
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        quote.symbol,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        FormatUtils.formatCurrency(quote.price),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    PercentageChange(value = quote.changePercent)
                                    Text(
                                        FormatUtils.formatPnl(quote.change),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (quote.change >= 0) ProfitGreen else LossRed
                                    )
                                }
                            }

                            if (uiState.selectedBars.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                CandlestickChart(
                                    opens = uiState.selectedBars.takeLast(30).map { it.open },
                                    highs = uiState.selectedBars.takeLast(30).map { it.high },
                                    lows = uiState.selectedBars.takeLast(30).map { it.low },
                                    closes = uiState.selectedBars.takeLast(30).map { it.close },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricCard(
                                    title = "Volume",
                                    value = FormatUtils.formatVolume(quote.volume),
                                    modifier = Modifier.weight(1f)
                                )
                                if (uiState.selectedBars.isNotEmpty()) {
                                    val bars = uiState.selectedBars
                                    MetricCard(
                                        title = "52W High",
                                        value = FormatUtils.formatCurrency(bars.maxOf { it.high }),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricCard(
                                        title = "52W Low",
                                        value = FormatUtils.formatCurrency(bars.minOf { it.low }),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.watchlist.isNotEmpty()) {
                item {
                    SectionHeader(title = "Watchlist")
                }

                items(uiState.watchlist) { item ->
                    val quote = uiState.watchlistQuotes[item.symbol]
                    StockTickerCard(
                        symbol = item.symbol,
                        name = item.name,
                        price = quote?.price ?: 0.0,
                        change = quote?.change ?: 0.0,
                        changePercent = quote?.changePercent ?: 0.0,
                        onClick = { viewModel.selectSymbol(item.symbol) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
