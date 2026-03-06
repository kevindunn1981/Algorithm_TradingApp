package com.algotrader.app.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Internal API response models
private data class ApiResponse<T>(val retType: Int, val retMsg: String, val data: T?)
private data class AccountFundsData(val totalAssets: Double, val cash: Double, val marketValue: Double)
private data class PositionData(val code: String, val name: String, val qty: Double, val costPrice: Double, val curPrice: Double)
private data class QuoteData(val code: String, val name: String, val curPrice: Double, val changeVal: Double, val changeRate: Double, val volume: Long)
private data class IndexData(val name: String, val curPrice: Double, val changeVal: Double, val changeRate: Double)

private interface MoomooOpenDApi {
    @POST("get_account_funds")
    suspend fun getAccountFunds(@Body request: Map<String, String>): ApiResponse<AccountFundsData>

    @POST("get_position_list")
    suspend fun getPositionList(@Body request: Map<String, String>): ApiResponse<List<PositionData>>

    @POST("get_stock_quote")
    suspend fun getStockQuote(@Body request: Map<String, Any>): ApiResponse<List<QuoteData>>

    @POST("get_market_snapshot")
    suspend fun getMarketSnapshot(@Body request: Map<String, Any>): ApiResponse<List<IndexData>>

    @POST("place_order")
    suspend fun placeOrder(@Body request: Map<String, Any>): ApiResponse<Map<String, String>>
}

class MoomooApiClient(private val credentials: CredentialManager) {

    private val api: MoomooOpenDApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("http://${credentials.openDHost}:${credentials.openDPort}/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MoomooOpenDApi::class.java)
    }

    suspend fun getAccountBalance(): Result<Double> = runCatching {
        val resp = api.getAccountFunds(mapOf("trd_env" to "REAL"))
        resp.data?.totalAssets ?: error("No data in response")
    }

    suspend fun getPortfolioHoldings(): Result<List<Holding>> = runCatching {
        val resp = api.getPositionList(mapOf("trd_env" to "REAL"))
        (resp.data ?: emptyList()).map { p ->
            Holding(
                ticker = p.code,
                name = p.name,
                shares = p.qty,
                avgCost = p.costPrice,
                currentPrice = p.curPrice
            )
        }
    }

    suspend fun getStockQuotes(tickers: List<String>): Result<List<Stock>> = runCatching {
        val resp = api.getStockQuote(mapOf("code_list" to tickers))
        (resp.data ?: emptyList()).map { q ->
            Stock(
                ticker = q.code,
                name = q.name,
                price = q.curPrice,
                change = q.changeVal,
                changePercent = q.changeRate,
                volume = q.volume.toString()
            )
        }
    }

    suspend fun getMarketIndices(): Result<List<MarketIndex>> = runCatching {
        val resp = api.getMarketSnapshot(mapOf("code_list" to listOf("US.SPX", "US.NDX", "US.DJI")))
        (resp.data ?: emptyList()).map { i ->
            MarketIndex(
                name = i.name,
                value = i.curPrice,
                change = i.changeVal,
                changePercent = i.changeRate
            )
        }
    }

    suspend fun placeOrder(ticker: String, quantity: Double, side: OrderSide): Result<String> = runCatching {
        val resp = api.placeOrder(
            mapOf(
                "code" to ticker,
                "qty" to quantity,
                "trd_side" to side.name,
                "trd_env" to "REAL"
            )
        )
        resp.data?.get("order_id") ?: error("No order_id in response")
    }
}
