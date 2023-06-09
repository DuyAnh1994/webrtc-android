package com.bglobal.lib.webrtc.data.socket

import android.util.Log
import com.bglobal.lib.utils.TAG
import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.bglobal.lib.webrtc.data.model.call.request.RtcDtoRequest
import com.bglobal.lib.webrtc.data.model.call.response.RtcDtoResponse
import com.bglobal.lib.webrtc.data.model.call.update.RtcDtoUpdate
import com.bglobal.lib.webrtc.data.model.event.response.EventDtoResponse
import com.google.gson.GsonBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class BglobalSocketClient(
    private val responseListener: BglobalSocketListener.Response,
    private val updateListener: BglobalSocketListener.Update,
    private val eventListener: BglobalSocketListener.Event,
) {

    companion object {
        private const val WS_URL = "wss://dev.turn2.gtrios.io:8084/?id=4"
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
                Log.d(TAG, "===> onMessage raw data from wss: $message")

                val baseResponse = gson.fromJson(message, RtcBaseResponse::class.java)
                when (baseResponse.type) {
                    "response" -> responseListener.onRtcResponse(gson.fromJson(message, RtcDtoResponse::class.java))

                    "cmd" -> {
                        when (baseResponse.name) {
                            "update" -> updateListener.onRtcUpdate(gson.fromJson(message, RtcDtoUpdate::class.java))
                        }
                    }

                    "event" -> {
                        when (baseResponse.name) {
                            "participants" -> {
                                val eventDto = gson.fromJson(message, EventDtoResponse::class.java)
                                eventListener.onParticipantList(eventDto.data ?: mutableListOf())
                            }
                        }
                    }
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
            Log.d(TAG, "send json : type=${rtcDto.type} name=${rtcDto.name} transId=${rtcDto.transId} userName=${rtcDto.dataDto?.name}")
            webSocket?.send(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
