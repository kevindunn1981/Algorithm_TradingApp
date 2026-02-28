# AlgoTrader - Algorithmic Trading App for Android

A comprehensive, open-source Android app for algorithmic and programming-based stock trading. Build, backtest, and deploy custom trading strategies directly from your phone.

## Features

### Strategy Builder & Code Editor
- **Built-in code editor** with syntax highlighting and line numbers
- **Kotlin DSL** for writing trading strategies with a clean, expressive API
- **5 pre-built strategy templates** ready to customize:
  - SMA Crossover (Golden/Death Cross)
  - RSI Mean Reversion (Oversold/Overbought)
  - MACD Signal Line Crossover
  - Bollinger Band Bounce (Mean Reversion)
  - Multi-Indicator Momentum (EMA + RSI)
- Create, edit, save, and manage multiple strategies
- Toggle strategies active/inactive for live execution

### Technical Indicators
Full library of technical analysis indicators computed in real-time:
- **SMA** - Simple Moving Average (configurable period)
- **EMA** - Exponential Moving Average (configurable period)
- **RSI** - Relative Strength Index
- **MACD** - Moving Average Convergence Divergence (with signal line and histogram)
- **Bollinger Bands** - Upper, middle, lower bands with bandwidth
- **VWAP** - Volume Weighted Average Price
- **ATR** - Average True Range
- **Stochastic Oscillator** - %K and %D lines

### Backtesting Engine
- Test strategies against historical market data
- Configurable parameters:
  - Symbol, timeframe (1Min to 1Day), lookback period
  - Initial capital, position sizing
  - Stop loss and take profit levels
- Comprehensive performance metrics:
  - Total/annualized return
  - Sharpe Ratio & Sortino Ratio
  - Maximum drawdown
  - Win rate, profit factor
  - Average/largest win and loss
- **Equity curve visualization**
- **Trade-by-trade breakdown** with entry/exit prices and P&L
- Realistic simulation with slippage and commission modeling
- Falls back to generated sample data when API data is unavailable

### Live & Paper Trading
- **Alpaca Markets API** integration for commission-free trading
- **Paper trading mode** for risk-free strategy testing
- Submit market, limit, stop, and trailing stop orders
- Real-time position tracking and P&L monitoring
- Order management (submit, cancel, view history)

### Market Data
- Real-time stock quotes and snapshots via Alpaca Market Data API
- **Candlestick charts** with OHLC data
- **Sparkline charts** for quick price visualization
- Symbol search and discovery
- Popular stocks quick-access chips
- Historical bar data with local caching

### Portfolio Dashboard
- Account overview: equity, cash, buying power
- Open positions with unrealized P&L
- Active strategy monitoring
- Win rate and total P&L tracking
- Trade history log

### Watchlist
- Add/remove favorite symbols
- Real-time price updates
- Change and percentage display
- Quick-tap to view detailed charts

### Settings & Configuration
- Alpaca API key management with secure input
- Paper/live trading mode toggle
- Connection testing
- Trading defaults (capital, position size, stop loss)
- Push notification preferences (trades, signals)
- Dark/light theme support

## Architecture

Built with modern Android development best practices:

```
com.algotrader.app/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Retrofit API interfaces, DTOs
│   ├── repository/     # Data repositories
│   └── model/          # Domain models
├── engine/
│   ├── indicators/     # Technical analysis indicators
│   ├── strategy/       # Strategy framework & DSL
│   └── BacktestEngine  # Backtesting simulation
├── ui/
│   ├── theme/          # Material 3 theming
│   ├── navigation/     # Jetpack Navigation
│   ├── components/     # Reusable UI components
│   └── screens/        # Feature screens with ViewModels
├── di/                 # Hilt dependency injection
└── util/               # Formatting utilities
```

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with StateFlow
- **DI**: Hilt (Dagger)
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines & Flows
- **Navigation**: Jetpack Navigation Compose
- **Charts**: Custom Canvas-based composables
- **Min SDK**: 26 (Android 8.0)

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd AlgoTrader
```

2. Open in Android Studio

3. Sync Gradle and build the project

4. Run on an emulator or physical device (API 26+)

### Alpaca API Setup

1. Create a free account at [alpaca.markets](https://alpaca.markets)
2. Generate API keys (paper trading recommended for testing)
3. In the app, go to **Settings** and enter your API credentials
4. Toggle **Paper Trading Mode** on for simulated trading
5. Tap **Test Connection** to verify

## Strategy DSL

Write custom strategies using the built-in Kotlin DSL:

```kotlin
strategy("My Custom Strategy") {
    describe("Buy on golden cross, sell on death cross")

    onBar { index ->
        val fast = sma(10)
        val slow = sma(30)

        when {
            crossOver(fast, slow, index) -> buy(reason = "Golden cross")
            crossUnder(fast, slow, index) -> sell(reason = "Death cross")
            else -> null
        }
    }
}
```

### Available DSL Functions

| Function | Description |
|----------|-------------|
| `sma(period)` | Simple Moving Average |
| `ema(period)` | Exponential Moving Average |
| `rsi(period)` | Relative Strength Index |
| `macd(fast, slow, signal)` | MACD with signal line |
| `bollingerBands(period, stdDev)` | Bollinger Bands |
| `atr(period)` | Average True Range |
| `vwap()` | Volume Weighted Average Price |
| `crossOver(series1, series2, index)` | Detect bullish crossover |
| `crossUnder(series1, series2, index)` | Detect bearish crossover |
| `buy(...)` | Generate buy signal |
| `sell(...)` | Generate sell signal |
| `close`, `open`, `high`, `low`, `volume` | Price data accessors |

## Inspired By

This app was designed with inspiration from leading algorithmic trading platforms:

- **[Roboquant](https://roboquant.org)** - Fast, flexible algorithmic trading framework
- **[Alpaca Ribbit](https://github.com/alpacahq/ribbit-android)** - Reference trading app
- **[Composer](https://composer.trade)** - AI-powered strategy builder
- **[Tradetron](https://tradetron.tech)** - Multi-broker algo trading
- **[Gunbot Quant](https://github.com/GuntharDeNiro/gunbot-quant)** - Quantitative trading toolkit

## License

This project is open source. See LICENSE for details.

## Disclaimer

This software is for educational and informational purposes only. It is not financial advice. Trading involves significant risk of loss. Always do your own research and consider consulting a financial advisor before making investment decisions. The developers are not responsible for any financial losses incurred through the use of this software.
