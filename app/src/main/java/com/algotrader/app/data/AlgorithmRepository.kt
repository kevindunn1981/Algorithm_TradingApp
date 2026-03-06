package com.algotrader.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlgorithmRepository(context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("algorithm_states", Context.MODE_PRIVATE)

    private val _algorithms = MutableStateFlow(loadAlgorithms())
    
    fun getAlgorithms(): Flow<List<TradingAlgorithm>> = _algorithms.asStateFlow()

    fun getAlgorithmsSnapshot(): List<TradingAlgorithm> = _algorithms.value

    fun toggleAlgorithm(name: String) {
        val updated = _algorithms.value.map { algo ->
            if (algo.name == name) {
                val newActive = !algo.isActive
                prefs.edit().putBoolean(name, newActive).apply()
                algo.copy(
                    isActive = newActive,
                    status = if (newActive) "Monitoring" else "Inactive"
                )
            } else algo
        }
        _algorithms.value = updated
    }

    private fun loadAlgorithms(): List<TradingAlgorithm> =
        MockDataProvider.algorithms.map { algo ->
            val savedActive = prefs.getBoolean(algo.name, algo.isActive)
            algo.copy(
                isActive = savedActive,
                status = if (savedActive) algo.status else "Inactive"
            )
        }
}
