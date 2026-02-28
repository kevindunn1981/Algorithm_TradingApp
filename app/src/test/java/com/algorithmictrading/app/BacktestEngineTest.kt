package com.algorithmictrading.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacktestEngineTest {
    private val engine = BacktestEngine()

    @Test
    fun run_clamps_lookback_to_minimum() {
        val result = engine.run(
            strategy = StrategyTemplate(
                id = "trend",
                name = "Trend",
                description = "test",
                timeframe = "1h",
                defaultParams = mapOf("lookback" to 20.0),
            ),
            lookbackDays = 1,
            startingCapital = 10_000.0,
            riskConfig = RiskConfig(),
        )

        assertEquals(30, result.tradeCount)
    }

    @Test
    fun run_returns_stable_deterministic_output_for_same_input() {
        val strategy = StrategyTemplate(
            id = "breakout",
            name = "Breakout",
            description = "test",
            timeframe = "1h",
            defaultParams = mapOf("atrMultiplier" to 1.5),
        )
        val risk = RiskConfig(maxPositionPct = 20.0)

        val first = engine.run(
            strategy = strategy,
            lookbackDays = 180,
            startingCapital = 10_000.0,
            riskConfig = risk,
        )
        val second = engine.run(
            strategy = strategy,
            lookbackDays = 180,
            startingCapital = 10_000.0,
            riskConfig = risk,
        )

        assertEquals(first.totalReturnPct, second.totalReturnPct, 0.000001)
        assertEquals(first.maxDrawdownPct, second.maxDrawdownPct, 0.000001)
        assertEquals(first.sharpeRatio, second.sharpeRatio, 0.000001)
    }

    @Test
    fun run_outputs_metrics_in_reasonable_ranges() {
        val result = engine.run(
            strategy = StrategyTemplate(
                id = "mean_rev",
                name = "Mean Reversion",
                description = "test",
                timeframe = "15m",
                defaultParams = mapOf("lookback" to 10.0),
            ),
            lookbackDays = 365,
            startingCapital = 25_000.0,
            riskConfig = RiskConfig(maxPositionPct = 15.0),
        )

        assertTrue(result.maxDrawdownPct in 0.0..100.0)
        assertTrue(result.winRatePct in 0.0..100.0)
        assertTrue(result.tradeCount == 365)
        assertTrue(result.endingCapital > 0.0)
    }
}
