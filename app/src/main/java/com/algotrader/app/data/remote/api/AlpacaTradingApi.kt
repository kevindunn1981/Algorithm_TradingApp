package com.algotrader.app.data.remote.api

import com.algotrader.app.data.remote.dto.AlpacaAccountDto
import com.algotrader.app.data.remote.dto.AlpacaAssetDto
import com.algotrader.app.data.remote.dto.AlpacaOrderDto
import com.algotrader.app.data.remote.dto.AlpacaOrderRequest
import com.algotrader.app.data.remote.dto.AlpacaPositionDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AlpacaTradingApi {

    @GET("v2/account")
    suspend fun getAccount(): AlpacaAccountDto

    @GET("v2/positions")
    suspend fun getPositions(): List<AlpacaPositionDto>

    @GET("v2/positions/{symbol}")
    suspend fun getPosition(@Path("symbol") symbol: String): AlpacaPositionDto

    @GET("v2/orders")
    suspend fun getOrders(
        @Query("status") status: String = "all",
        @Query("limit") limit: Int = 100,
        @Query("direction") direction: String = "desc"
    ): List<AlpacaOrderDto>

    @POST("v2/orders")
    suspend fun submitOrder(@Body order: AlpacaOrderRequest): AlpacaOrderDto

    @DELETE("v2/orders/{orderId}")
    suspend fun cancelOrder(@Path("orderId") orderId: String)

    @DELETE("v2/orders")
    suspend fun cancelAllOrders()

    @DELETE("v2/positions/{symbol}")
    suspend fun closePosition(@Path("symbol") symbol: String): AlpacaOrderDto

    @DELETE("v2/positions")
    suspend fun closeAllPositions()

    @GET("v2/assets")
    suspend fun getAssets(
        @Query("status") status: String = "active",
        @Query("asset_class") assetClass: String = "us_equity"
    ): List<AlpacaAssetDto>

    @GET("v2/assets/{symbol}")
    suspend fun getAsset(@Path("symbol") symbol: String): AlpacaAssetDto
}
