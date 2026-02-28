package com.algotrader.app.data.remote.api

import com.algotrader.app.data.remote.dto.AlpacaBarsResponse
import com.algotrader.app.data.remote.dto.AlpacaSnapshotDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AlpacaMarketDataApi {

    @GET("v2/stocks/{symbol}/bars")
    suspend fun getBars(
        @Path("symbol") symbol: String,
        @Query("timeframe") timeframe: String,
        @Query("start") start: String,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int = 1000,
        @Query("adjustment") adjustment: String = "split",
        @Query("page_token") pageToken: String? = null
    ): AlpacaBarsResponse

    @GET("v2/stocks/{symbol}/snapshot")
    suspend fun getSnapshot(
        @Path("symbol") symbol: String
    ): AlpacaSnapshotDto

    @GET("v2/stocks/snapshots")
    suspend fun getSnapshots(
        @Query("symbols") symbols: String
    ): Map<String, AlpacaSnapshotDto>

    @GET("v2/stocks/bars")
    suspend fun getMultiBars(
        @Query("symbols") symbols: String,
        @Query("timeframe") timeframe: String,
        @Query("start") start: String,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int = 1000
    ): AlpacaBarsResponse
}
