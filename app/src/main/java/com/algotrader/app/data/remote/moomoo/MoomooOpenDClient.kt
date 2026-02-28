package com.algotrader.app.data.remote.moomoo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class MoomooOpenDClient(
    private val host: String = "127.0.0.1",
    private val port: Int = 33333,
    private val useWebSocket: Boolean = true
) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serialCounter = AtomicInteger(0)
    private val connected = AtomicBoolean(false)
    private val mutex = Mutex()
    private val pendingRequests = ConcurrentHashMap<Int, CompletableDeferred<MoomooResponse>>()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    var onConnectionChanged: ((Boolean) -> Unit)? = null
    var onOrderUpdate: ((MoomooOrderItem) -> Unit)? = null
    var onQuoteUpdate: ((MoomooQuoteItem) -> Unit)? = null

    fun isConnected(): Boolean = connected.get()

    suspend fun connect(): Boolean {
        if (connected.get()) return true

        return try {
            val wsUrl = if (useWebSocket) "ws://$host:$port" else "wss://$host:$port"
            val request = Request.Builder().url(wsUrl).build()

            val deferred = CompletableDeferred<Boolean>()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connected.set(true)
                    onConnectionChanged?.invoke(true)
                    scope.launch { initConnection() }
                    deferred.complete(true)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    connected.set(false)
                    onConnectionChanged?.invoke(false)
                    pendingRequests.values.forEach { it.completeExceptionally(t) }
                    pendingRequests.clear()
                    if (!deferred.isCompleted) deferred.complete(false)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    connected.set(false)
                    onConnectionChanged?.invoke(false)
                }
            })

            withTimeout(10_000) { deferred.await() }
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        connected.set(false)
        onConnectionChanged?.invoke(false)
    }

    private suspend fun initConnection() {
        val initReq = mapOf(
            "clientVer" to 300,
            "clientID" to "AlgoTrader-Android",
            "recvNotify" to true
        )
        sendRequest(MoomooConstants.PROTO_INIT_CONNECT, initReq)

        scope.launch {
            while (connected.get()) {
                delay(15_000)
                if (connected.get()) {
                    try {
                        sendRequest(MoomooConstants.PROTO_KEEP_ALIVE, mapOf("time" to System.currentTimeMillis()))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    suspend fun sendRequest(protocolId: Int, params: Map<String, Any>): MoomooResponse {
        val serialNo = serialCounter.incrementAndGet()
        val request = mapOf(
            "c2s" to params,
            "protoId" to protocolId,
            "serialNo" to serialNo
        )
        val json = gson.toJson(request)

        val deferred = CompletableDeferred<MoomooResponse>()
        pendingRequests[serialNo] = deferred

        mutex.withLock {
            val sent = webSocket?.send(json) ?: false
            if (!sent) {
                pendingRequests.remove(serialNo)
                throw Exception("Failed to send message to OpenD")
            }
        }

        return try {
            withTimeout(30_000) { deferred.await() }
        } finally {
            pendingRequests.remove(serialNo)
        }
    }

    private fun handleMessage(text: String) {
        try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val rawMap: Map<String, Any> = gson.fromJson(text, type)

            val serialNo = (rawMap["serialNo"] as? Number)?.toInt() ?: 0
            val retCode = (rawMap["retType"] as? Number)?.toInt() ?: 0
            val retMsg = rawMap["retMsg"] as? String ?: ""
            val errCode = (rawMap["errCode"] as? Number)?.toInt() ?: 0

            @Suppress("UNCHECKED_CAST")
            val s2c = rawMap["s2c"] as? Map<String, Any>

            val response = MoomooResponse(
                retCode = retCode,
                retMsg = retMsg,
                errCode = errCode,
                data = s2c
            )

            val pending = pendingRequests.remove(serialNo)
            if (pending != null) {
                pending.complete(response)
            } else {
                handlePushNotification(rawMap)
            }
        } catch (e: Exception) {
            // Malformed message
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handlePushNotification(data: Map<String, Any>) {
        val protoId = (data["protoId"] as? Number)?.toInt() ?: return
        val s2c = data["s2c"] as? Map<String, Any> ?: return

        when (protoId) {
            MoomooConstants.PROTO_TRD_GET_ORDER_LIST -> {
                try {
                    val orderJson = gson.toJson(s2c)
                    val orderItem = gson.fromJson(orderJson, MoomooOrderItem::class.java)
                    onOrderUpdate?.invoke(orderItem)
                } catch (_: Exception) {}
            }
            MoomooConstants.PROTO_QOT_GET_STOCK_QUOTE -> {
                try {
                    val quoteJson = gson.toJson(s2c)
                    val quoteItem = gson.fromJson(quoteJson, MoomooQuoteItem::class.java)
                    onQuoteUpdate?.invoke(quoteItem)
                } catch (_: Exception) {}
            }
        }
    }

    // ── Convenience methods for common operations ────────────────────────

    suspend fun getAccountList(tradingEnv: Int = MoomooConstants.TRD_ENV_SIMULATE): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_TRD_GET_ACC_LIST, mapOf("trdEnv" to tradingEnv))
    }

    suspend fun unlockTrade(passwordMd5: String, isSaveUnlock: Boolean = false): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_TRD_UNLOCK_TRADE, mapOf(
            "unlock" to true,
            "pwdMD5" to passwordMd5,
            "securityFirm" to 1,
            "isSaveUnlock" to isSaveUnlock
        ))
    }

    suspend fun getAccountFunds(
        accountId: Long,
        tradingEnv: Int = MoomooConstants.TRD_ENV_SIMULATE
    ): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_TRD_GET_FUNDS, mapOf(
            "header" to mapOf(
                "accID" to accountId,
                "trdEnv" to tradingEnv,
                "trdMarket" to MoomooConstants.TRD_MARKET_US
            )
        ))
    }

    suspend fun getPositions(
        accountId: Long,
        tradingEnv: Int = MoomooConstants.TRD_ENV_SIMULATE,
        market: Int = MoomooConstants.TRD_MARKET_US
    ): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_TRD_GET_POSITIONS, mapOf(
            "header" to mapOf(
                "accID" to accountId,
                "trdEnv" to tradingEnv,
                "trdMarket" to market
            )
        ))
    }

    suspend fun getOrderList(
        accountId: Long,
        tradingEnv: Int = MoomooConstants.TRD_ENV_SIMULATE,
        market: Int = MoomooConstants.TRD_MARKET_US
    ): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_TRD_GET_ORDER_LIST, mapOf(
            "header" to mapOf(
                "accID" to accountId,
                "trdEnv" to tradingEnv,
                "trdMarket" to market
            )
        ))
    }

    suspend fun placeOrder(request: MoomooPlaceOrderRequest): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_TRD_PLACE_ORDER, mapOf(
            "header" to mapOf(
                "accID" to request.accountId,
                "trdEnv" to request.tradingEnvironment,
                "trdMarket" to request.market
            ),
            "trdSide" to request.side,
            "orderType" to request.orderType,
            "code" to request.code,
            "qty" to request.quantity,
            "price" to request.price,
            "adjustPrice" to request.adjustPrice,
            "secMarket" to request.market
        ))
    }

    suspend fun modifyOrder(request: MoomooModifyOrderRequest): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_TRD_MODIFY_ORDER, mapOf(
            "header" to mapOf(
                "accID" to request.accountId,
                "trdEnv" to request.tradingEnvironment,
                "trdMarket" to MoomooConstants.TRD_MARKET_US
            ),
            "orderID" to request.orderId,
            "modifyOrderOp" to request.operation,
            "qty" to (request.quantity ?: 0),
            "price" to (request.price ?: 0)
        ))
    }

    suspend fun getMarketSnapshot(codes: List<String>): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_QOT_GET_MARKET_SNAPSHOT, mapOf(
            "securityList" to codes.map { mapOf("code" to it, "market" to 11) }
        ))
    }

    suspend fun getStockQuote(codes: List<String>): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_QOT_GET_STOCK_QUOTE, mapOf(
            "securityList" to codes.map { mapOf("code" to it, "market" to 11) }
        ))
    }

    suspend fun requestHistoryKLine(
        code: String,
        klineType: Int = MoomooConstants.KLINE_DAY,
        start: String? = null,
        end: String? = null,
        maxCount: Int = 1000,
        pageReqKey: String? = null
    ): MoomooResponse {
        val params = mutableMapOf<String, Any>(
            "security" to mapOf("code" to code, "market" to 11),
            "klType" to klineType,
            "reqNum" to maxCount
        )
        start?.let { params["beginTime"] = it }
        end?.let { params["endTime"] = it }
        pageReqKey?.let { params["nextPageReqKey"] = it }

        return sendRequest(MoomooConstants.PROTO_QOT_REQUEST_HISTORY_KLINE, params)
    }

    suspend fun subscribe(codes: List<String>, subTypes: List<Int>): MoomooResponse {
        return sendRequest(MoomooConstants.PROTO_QOT_SUB, mapOf(
            "securityList" to codes.map { mapOf("code" to it, "market" to 11) },
            "subTypeList" to subTypes,
            "isSubOrUnSub" to true
        ))
    }
}
