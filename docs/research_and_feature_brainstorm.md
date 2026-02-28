# Research: successful trading apps and GitHub projects

This document summarizes reference products and projects used to shape the Android app MVP in this repository.

## 1) Successful internet apps studied

### TradingView (mobile)
- Source: Google Play + TradingView mobile page
- Strengths:
  - Advanced charting + technical studies
  - Real-time watchlists and alerts
  - Social idea sharing
- Product takeaway:
  - Traders need fast quote monitoring and chart-first workflows.

### thinkorswim Mobile (Charles Schwab)
- Source: Google Play / App Store listing
- Strengths:
  - Paper trading (paperMoney)
  - Advanced order types and conditional logic
  - Multi-asset support
- Product takeaway:
  - A safe simulation environment is critical before live execution.

### MetaTrader 5 Mobile
- Source: MetaTrader 5 mobile information pages
- Strengths:
  - Signals and algorithmic ecosystem
  - Broad broker connectivity
  - Built-in analysis + execution workflow
- Product takeaway:
  - Broker abstraction and strategy deployment pipelines matter for scale.

## 2) Successful GitHub projects studied

### QuantConnect/Lean
- URL: https://github.com/QuantConnect/Lean
- Snapshot: ~17k stars, open-source algorithmic engine (C#/Python)
- Strengths:
  - Backtest/live parity
  - Pluggable data + brokerage model
  - Strong risk and portfolio abstractions
- Product takeaway:
  - Build modular strategy and execution layers.

### freqtrade/freqtrade
- URL: https://github.com/freqtrade/freqtrade
- Snapshot: ~47k stars, Python trading bot framework
- Strengths:
  - Dry-run mode
  - Strategy optimization
  - Operational controls and monitoring
- Product takeaway:
  - Paper-first workflow + guardrails should be default.

### hummingbot/hummingbot
- URL: https://github.com/hummingbot/hummingbot
- Snapshot: ~17k stars, high-frequency crypto bot framework
- Strengths:
  - Connector architecture
  - Strategy scripts/controllers
  - Automation-focused deployments
- Product takeaway:
  - Strategy and broker connectors should be swappable.

### rohnsha0/StockSense-androidApp
- URL: https://github.com/rohnsha0/StockSense-androidApp
- Snapshot: Kotlin Android app focused on stock analysis/prediction
- Strengths:
  - Android-native UX for market users
  - Technical indicator exploration
- Product takeaway:
  - Keep Android UX lightweight and insight-focused.

## 3) Feature brainstorm (prioritized)

## MVP (implemented in this repo)
1. **Strategy Lab**
   - Toggle/select strategy templates.
2. **Backtest tab**
   - Run quick scenario simulation with performance metrics.
3. **Paper Trading tab**
   - Simulated buy/sell against live-ish quotes.
4. **Risk Controls tab**
   - Max daily loss, max position size, max open positions, kill switch.
5. **Dashboard**
   - Quotes, account summary, and activity feed.

## Phase 2
1. Candlestick charting with indicators (SMA/EMA/RSI/MACD/Bollinger).
2. Broker API connectors (Alpaca, Interactive Brokers, TD, Zerodha, etc.).
3. Strategy editor (Kotlin/Python DSL sandbox).
4. Alerts and webhook triggers.
5. Secure credential vaulting + audit logs.

## Phase 3
1. Portfolio optimization / risk models (VaR, CVaR, Kelly variants).
2. Walk-forward analysis and hyperparameter search.
3. Social strategy sharing and leaderboards.
4. Cloud execution and failover monitoring.

## 4) Design principles extracted from research

1. **Paper-first execution safety** (thinkorswim/freqtrade influence).
2. **Risk controls are first-class, not optional** (Lean institutional style).
3. **Modular strategy architecture** (Lean/Hummingbot connector mindset).
4. **Fast mobile feedback loops** (TradingView UX influence).
