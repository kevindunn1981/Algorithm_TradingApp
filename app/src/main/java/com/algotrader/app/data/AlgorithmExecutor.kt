package com.algotrader.app.data

enum class OrderSide { BUY, SELL }

class AlgorithmExecutor(
    private val apiClient: MoomooApiClient,
    private val algorithmRepository: AlgorithmRepository
) {
    suspend fun executeSignal(algorithmName: String, ticker: String, quantity: Double, side: OrderSide): Result<String> {
        val algorithms = algorithmRepository.getAlgorithmsSnapshot()
        val algorithm = algorithms.find { it.name == algorithmName }
            ?: return Result.failure(IllegalArgumentException("Algorithm '$algorithmName' not found"))
        if (!algorithm.isActive) return Result.failure(IllegalStateException("Algorithm '$algorithmName' is not active"))
        return apiClient.placeOrder(ticker, quantity, side)
    }
}
