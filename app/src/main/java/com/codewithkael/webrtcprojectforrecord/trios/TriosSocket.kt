package com.codewithkael.webrtcprojectforrecord.trios

import android.util.Log
import com.codewithkael.webrtcprojectforrecord.trios.model.base.RtcBaseResponse
import com.codewithkael.webrtcprojectforrecord.trios.model.call.request.RtcDtoRequest
import com.codewithkael.webrtcprojectforrecord.trios.model.call.request.RtcDtoResponse2
import com.codewithkael.webrtcprojectforrecord.trios.model.call.response.RtcDtoResponse
import com.codewithkael.webrtcprojectforrecord.trios.model.call.update.RtcDtoUpdate
import com.codewithkael.webrtcprojectforrecord.trios.model.event.response.EventDtoResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI


class TriosSocket(private val listener: TriosSocketListener) {

    companion object {
        private const val TAG = "TriosSocket"
        private const val WS_URL = "wss://dev.turn2.gtrios.io:8084/?id=5"
//        private const val WS_URL = "http://localhost:8080"
    }

    private var webSocket: WebSocketClient? = null
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    init {
        initSocket()
    }

    private fun initSocket() {
        webSocket = object : WebSocketClient(URI(WS_URL)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d(TAG, "onOpen: connecting  httpStatus: ${handshakedata?.httpStatus}  httpMsg: ${handshakedata?.httpStatusMessage}")
            }

            override fun onMessage(message: String?) {
                Log.d(TAG, "onMessage raw data: $message")
                val baseResponse = gson.fromJson(message, RtcBaseResponse::class.java)
                when (baseResponse.type) {
                    "cmd" -> {
                        when (baseResponse.name) {
                            "update" -> listener.onRtcUpdate(gson.fromJson(message, RtcDtoUpdate::class.java))
                        }
                    }

                    "response" -> listener.onRtcResponse(gson.fromJson(message, RtcDtoResponse::class.java))
                    "event" -> listener.onRtcEvent(gson.fromJson(message, EventDtoResponse::class.java))
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG, "onClose() called with: code = $code, reason = $reason, remote = $remote")
            }

            override fun onError(ex: Exception?) {
                ex?.printStackTrace()
                Log.d(TAG, "onError: ${ex?.message}")
            }
        }

        webSocket?.connect()
    }

    fun sendMessageToSocket(rtcDto: RtcDtoRequest) {
        try {
            val json = gson.toJson(rtcDto)
            Log.d(TAG, "send json: $json")
            webSocket?.send(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessageToSocket(rtcDto: RtcDtoResponse2) {
        try {
            val json = gson.toJson(rtcDto)
            Log.d(TAG, "send json: $json")
            webSocket?.send(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
