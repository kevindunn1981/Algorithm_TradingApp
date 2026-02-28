# AlgoTrader - Algorithmic Trading App for Android

A comprehensive, open-source Android app for algorithmic and programming-based stock trading. Build, backtest, and deploy custom trading strategies directly from your phone. Supports multiple brokers including **Alpaca** and **Moomoo (Futu OpenAPI)**.

## Features

### Multi-Broker Support
Switch between brokers at runtime from the Settings screen:

| Feature | Alpaca | Moomoo |
|---------|--------|--------|
| US Stocks & ETFs | Yes | Yes |
| Hong Kong Stocks | -- | Yes |
| China A-Shares | -- | Yes |
| Singapore / Japan / Australia | -- | Yes |
| Options & Futures | -- | Yes |
| Fractional Shares | Yes | -- |
| Commission-Free | Yes | -- |
| Paper Trading | Yes | Yes |
| Level 2 Depth | -- | Yes (60 levels) |
| Historical Data | Yes | Up to 20 years |
| Connection | REST API | OpenD Gateway (WebSocket) |

### Strategy Builder & Code Editor
- **Built-in code editor** with syntax highlighting and line numbers
- **Kotlin DSL** for writing trading strategies with a clean, expressive API
- **22 pre-built strategy templates** organized by category (see below)
- Create, edit, save, and manage multiple strategies
- Toggle strategies active/inactive for live execution

### 22 Built-in Strategy Templates

**Trend Following (8 strategies):**
| Strategy | Description | Key Indicators |
|----------|-------------|----------------|
| SMA Crossover | Classic golden/death cross | SMA fast/slow |
| MACD Crossover | Signal line crossover with histogram | MACD, Signal, Histogram |
| Supertrend | ATR-based dynamic support/resistance flips | Supertrend (ATR) |
| ADX Trend Following | DI+/DI- crossover filtered by ADX strength | ADX, DI+, DI- |
| Turtle Trading | Richard Dennis' Donchian breakout system | Donchian Channel, ATR |
| Donchian Channel Breakout | Channel breakout with EMA trend filter | Donchian, EMA |
| Ichimoku Cloud | Tenkan/Kijun cross filtered by Kumo cloud | Tenkan, Kijun, Senkou A/B |
| Triple EMA Crossover | 3 EMA ribbon + RSI momentum + volume | EMA(8/21/55), RSI, Volume |

**Mean Reversion (7 strategies):**
| Strategy | Description | Key Indicators |
|----------|-------------|----------------|
| RSI Mean Reversion | Oversold/overbought reversal signals | RSI |
| Bollinger Band Bounce | Mean reversion at Bollinger band extremes | Bollinger Bands |
| Keltner Channel Mean Reversion | EMA+ATR band bounce with RSI confirmation | Keltner Channel, RSI |
| VWAP Mean Reversion | Fade price deviations from VWAP | VWAP |
| Z-Score Mean Reversion | Statistical z-score deviation trading | Rolling Z-Score |
| CCI Trend & Reversal | Commodity Channel Index signals with trend | CCI, SMA |
| Williams %R | Percent Range reversals with trend filter | Williams %R, EMA |

**Multi-Indicator / Advanced (3 strategies):**
| Strategy | Description | Key Indicators |
|----------|-------------|----------------|
| Multi-Indicator Momentum | Combined EMA trend + RSI momentum | EMA, RSI |
| Mean Reversion Confluence | 2-of-3 confluence: RSI + BB + MACD | RSI, Bollinger, MACD |
| Elder Triple Screen | Multi-timeframe: trend + pullback + trigger | EMA, MACD, Stochastic |

**Breakout / Momentum (4 strategies):**
| Strategy | Description | Key Indicators |
|----------|-------------|----------------|
| Stochastic Momentum | %K/%D cross at extremes + 200 EMA trend | Stochastic, EMA(200) |
| Opening Range Breakout | N-bar range breakout with volume confirmation | Price Range, Volume |
| Breakout Momentum | Volume-confirmed breakout + EMA alignment | Price, Volume, EMA |
| Dual MA + Volume Filter | EMA crossover with above-average volume | EMA(20/50), Volume |

### 15 Technical Indicators
Full library of technical analysis indicators computed in real-time:
- **SMA** - Simple Moving Average (configurable period)
- **EMA** - Exponential Moving Average (configurable period)
- **RSI** - Relative Strength Index
- **MACD** - Moving Average Convergence Divergence (with signal line and histogram)
- **Bollinger Bands** - Upper, middle, lower bands with bandwidth
- **VWAP** - Volume Weighted Average Price
- **ATR** - Average True Range
- **Stochastic Oscillator** - %K and %D lines
- **Donchian Channel** - Highest high / lowest low channel
- **Ichimoku Cloud** - Tenkan-sen, Kijun-sen, Senkou Span A/B, Chikou Span
- **Keltner Channel** - EMA + ATR-based volatility bands
- **ADX** - Average Directional Index with DI+ and DI-
- **Supertrend** - ATR-based trend direction with dynamic levels
- **Williams %R** - Percent Range momentum oscillator
- **CCI** - Commodity Channel Index

### Backtesting Engine
- Test strategies against historical market data from either broker
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
- **Alpaca Markets API** - Commission-free US stock/ETF trading via REST
- **Moomoo OpenAPI** - Multi-market trading via OpenD WebSocket gateway
- Paper trading mode for risk-free strategy testing on both brokers
- Submit market, limit, stop, and trailing stop orders
- Real-time position tracking and P&L monitoring
- Order management (submit, cancel, view history)

### Market Data
- Real-time stock quotes and snapshots from either broker
- **Candlestick charts** with OHLC data
- **Sparkline charts** for quick price visualization
- Symbol search and discovery
- Popular stocks quick-access chips
- Historical bar data with local caching
- Moomoo: Up to 20 years of daily candlestick history

### Portfolio Dashboard
- Account overview: equity, cash, buying power
- Open positions with unrealized P&L
- Active strategy monitoring
- Active broker indicator
- Win rate and total P&L tracking
- Trade history log

### Watchlist
- Add/remove favorite symbols
- Real-time price updates
- Change and percentage display
- Quick-tap to view detailed charts

### Settings & Configuration
- **Broker selection** with feature comparison cards
- Alpaca: API key/secret management
- Moomoo: OpenD host/port, account ID, market selection (US/HK/CN/SG/JP/AU)
- Paper/live trading mode toggle
- Connection testing for both brokers
- Trading defaults (capital, position size, stop loss)
- Push notification preferences (trades, signals)
- Dark/light theme support

## Architecture

Built with modern Android development best practices:

```
com.algotrader.app/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/
│   │   ├── api/        # Alpaca REST API interfaces
│   │   ├── dto/        # Alpaca DTOs
│   │   ├── moomoo/     # Moomoo OpenD client, DTOs, constants
│   │   └── broker/     # Broker abstraction layer
│   ├── repository/     # Data repositories (broker-agnostic)
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

### Broker Abstraction Layer

The app uses a clean abstraction to support multiple brokers:

```
BrokerProvider (interface)
├── AlpacaBrokerProvider  ─── Alpaca REST API (Retrofit)
└── MoomooBrokerProvider  ─── Moomoo OpenD (WebSocket)

MarketDataProvider (interface)
├── AlpacaBrokerProvider  ─── Alpaca Market Data API
└── MoomooBrokerProvider  ─── Moomoo Quote/KLine API

BrokerManager ─── Runtime broker switching & delegation
```

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with StateFlow
- **DI**: Hilt (Dagger)
- **Database**: Room
- **Networking**: Retrofit + OkHttp (Alpaca), WebSocket (Moomoo)
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

### Alpaca Setup

1. Create a free account at [alpaca.markets](https://alpaca.markets)
2. Generate API keys (paper trading recommended for testing)
3. In the app, go to **Settings** > select **Alpaca** as broker
4. Enter your API key and secret
5. Toggle **Paper Trading Mode** on for simulated trading
6. Tap **Test Connection** to verify

### Moomoo (Futu OpenAPI) Setup

1. Download and install **OpenD** from [openapi.moomoo.com](https://openapi.moomoo.com)
2. Create a Moomoo/Futu account and complete KYC verification
3. Launch OpenD on your computer or cloud server
4. Configure OpenD to enable WebSocket access:
   - Set WebSocket IP (use `0.0.0.0` for network access)
   - Set WebSocket port (default: `33333`)
5. In the app, go to **Settings** > select **Moomoo** as broker
6. Enter the OpenD host IP and port
7. Select your trading market (US, HK, CN, SG, JP, AU)
8. Toggle **Paper Trading** for simulated environment
9. Tap **Test Connection** to verify

#### Moomoo API Features
- **Protocol**: WebSocket with JSON messages to OpenD gateway
- **Markets**: US, Hong Kong, China A-Shares, Singapore, Japan, Australia
- **Order Types**: Market, Limit, Absolute Limit, Auction, Special Limit
- **Data**: Real-time quotes, snapshots, K-lines (1min to yearly), Level 2 depth
- **Push**: Real-time order status and quote updates via WebSocket
- **History**: Up to 20 years of daily candlestick data
- **Paper Trading**: Full simulated environment support

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
| `macd(fast, slow, signal)` | MACD with signal line and histogram |
| `bollingerBands(period, stdDev)` | Bollinger Bands (upper, middle, lower, bandwidth) |
| `atr(period)` | Average True Range |
| `vwap()` | Volume Weighted Average Price |
| `cci(period)` | Commodity Channel Index |
| `williamsR(period)` | Williams Percent Range |
| `donchianChannel(period)` | Donchian Channel (upper, lower, middle) |
| `keltnerChannel(emaPeriod, atrMult)` | Keltner Channel (EMA + ATR bands) |
| `supertrend(atrPeriod, multiplier)` | Supertrend with direction and bands |
| `crossOver(series1, series2, index)` | Detect bullish crossover |
| `crossUnder(series1, series2, index)` | Detect bearish crossover |
| `buy(...)` | Generate buy signal |
| `sell(...)` | Generate sell signal |
| `close`, `open`, `high`, `low`, `volume` | Price data accessors |

## Inspired By

This app was designed with inspiration from leading algorithmic trading platforms:

- **[Roboquant](https://roboquant.org)** - Fast, flexible algorithmic trading framework
- **[Alpaca Ribbit](https://github.com/alpacahq/ribbit-android)** - Reference trading app
- **[Moomoo OpenAPI](https://openapi.moomoo.com)** - Multi-market trading gateway
- **[Composer](https://composer.trade)** - AI-powered strategy builder
- **[Tradetron](https://tradetron.tech)** - Multi-broker algo trading
- **[Gunbot Quant](https://github.com/GuntharDeNiro/gunbot-quant)** - Quantitative trading toolkit

## License

This project is open source. See LICENSE for details.

## Disclaimer

This software is for educational and informational purposes only. It is not financial advice. Trading involves significant risk of loss. Always do your own research and consider consulting a financial advisor before making investment decisions. The developers are not responsible for any financial losses incurred through the use of this software.
