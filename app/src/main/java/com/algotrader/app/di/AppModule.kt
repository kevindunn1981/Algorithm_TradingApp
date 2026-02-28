package com.algotrader.app.di

import android.content.Context
import androidx.room.Room
import com.algotrader.app.data.local.AppDatabase
import com.algotrader.app.data.local.dao.BacktestResultDao
import com.algotrader.app.data.local.dao.PriceBarDao
import com.algotrader.app.data.local.dao.StrategyDao
import com.algotrader.app.data.local.dao.TradeDao
import com.algotrader.app.data.local.dao.WatchlistDao
import com.algotrader.app.data.remote.api.AlpacaMarketDataApi
import com.algotrader.app.data.remote.api.AlpacaTradingApi
import com.algotrader.app.data.remote.moomoo.MoomooOpenDClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val TRADING_BASE_URL = "https://paper-api.alpaca.markets/"
    private const val MARKET_DATA_BASE_URL = "https://data.alpaca.markets/"

    // ── Database ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "algotrader_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideStrategyDao(db: AppDatabase): StrategyDao = db.strategyDao()

    @Provides
    fun provideTradeDao(db: AppDatabase): TradeDao = db.tradeDao()

    @Provides
    fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    fun providePriceBarDao(db: AppDatabase): PriceBarDao = db.priceBarDao()

    @Provides
    fun provideBacktestResultDao(db: AppDatabase): BacktestResultDao = db.backtestResultDao()

    // ── Alpaca Networking ───────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("APCA-API-KEY-ID", "")
                .addHeader("APCA-API-SECRET-KEY", "")
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAlpacaTradingApi(client: OkHttpClient): AlpacaTradingApi {
        return Retrofit.Builder()
            .baseUrl(TRADING_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlpacaTradingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAlpacaMarketDataApi(client: OkHttpClient): AlpacaMarketDataApi {
        return Retrofit.Builder()
            .baseUrl(MARKET_DATA_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlpacaMarketDataApi::class.java)
    }

    // ── Moomoo Networking ───────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMoomooOpenDClient(): MoomooOpenDClient {
        return MoomooOpenDClient()
    }
}
