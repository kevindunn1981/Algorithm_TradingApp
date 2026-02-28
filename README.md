# Algorithmic Trading Android App

Android app prototype for algorithmic / programming-based stock trading workflows.

## What this project includes

- Kotlin + Jetpack Compose Android app scaffold
- Multi-tab trading workflow:
  - **Dashboard** (quotes, account snapshot, activity)
  - **Strategies** (strategy templates and enable/disable)
  - **Backtest** (quick simulation metrics)
  - **Paper Trade** (simulated buy/sell execution)
  - **Risk** (risk limits + kill switch)
- Simple domain engines:
  - Market quote simulator
  - Backtest engine
  - Paper-trading execution + position tracking
  - Risk validation rules

## Research foundation

Feature brainstorming is based on successful internet apps and GitHub projects:

- TradingView
- thinkorswim Mobile
- MetaTrader 5 Mobile
- QuantConnect Lean
- Freqtrade
- Hummingbot
- StockSense Android

Detailed notes: [`docs/research_and_feature_brainstorm.md`](docs/research_and_feature_brainstorm.md)

## Project structure

```text
app/
  src/main/
    java/com/algorithmictrading/app/
      MainActivity.kt
      TradingApp.kt
    AndroidManifest.xml
```

## Open in Android Studio

1. Open this repository in Android Studio.
2. Let Android Studio sync Gradle dependencies.
3. Run the `app` configuration on an emulator/device.

## Next roadmap ideas

- Real market data integration
- Broker API connectors
- Candlestick charts + technical indicators
- Strategy scripting sandbox
- Alerts + notifications
