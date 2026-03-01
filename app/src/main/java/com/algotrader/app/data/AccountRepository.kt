package com.algotrader.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AccountRepository(context: Context) {

    private val credentials = CredentialManager(context)
    private val apiClient = MoomooApiClient(credentials)

    fun getAccountBalance(): Flow<Double> = flow {
        if (credentials.isConfigured) {
            val result = apiClient.getAccountBalance()
            emit(result.getOrDefault(37_700.00))
        } else {
            emit(37_700.00)
        }
    }

    fun getHoldings(): Flow<List<Holding>> = flow {
        if (credentials.isConfigured) {
            val result = apiClient.getPortfolioHoldings()
            emit(result.getOrDefault(MockDataProvider.holdings))
        } else {
            emit(MockDataProvider.holdings)
        }
    }
}
