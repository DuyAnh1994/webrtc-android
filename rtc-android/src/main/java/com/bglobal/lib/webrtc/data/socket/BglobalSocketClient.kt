package com.bglobal.lib.webrtc.data.socket

import android.util.Log
import com.bglobal.lib.utils.TAG
import com.bglobal.lib.webrtc.data.model.base.RtcBaseRequest
import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.bglobal.lib.webrtc.data.model.call.ParticipantDTO
import com.bglobal.lib.webrtc.data.model.call.answer.AnswerResponse
import com.bglobal.lib.webrtc.data.model.call.offer.OfferResponse
import com.bglobal.lib.webrtc.data.model.call.participant.ParticipantResponse
import com.bglobal.lib.webrtc.data.model.call.peer.PeerBridgeMap
import com.bglobal.lib.webrtc.data.model.call.peer.PeerResponse
import com.google.gson.GsonBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class BglobalSocketClient(
    private val commandListener: BglobalSocketListener.Command,
    private val responseListener: BglobalSocketListener.Response,
    private val eventListener: BglobalSocketListener.Event,
    private val errorListener: BglobalSocketListener.Error,
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
                    SOCKET_TYPE.COMMAND -> onMsgByCommand(message, baseResponse.topic)
                    SOCKET_TYPE.RESPONSE -> onMsgByResponse(message, baseResponse.topic)
                    SOCKET_TYPE.EVENT -> onMsgByEvent(message, baseResponse.topic)
                    SOCKET_TYPE.ERROR -> onMsgByError(message, baseResponse.topic)
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

    private fun onMsgByCommand(rawData: String?, topic: String?) {
        Log.d(TAG, "onMsgByCommand: topic= $topic ---  rawData= $rawData")

        when (topic) {
            SOCKET_TOPIC.UPDATE -> {
//                Log.d(TAG, "onMsgByCommand UPDATE rawData: $rawData")
                val response = gson.fromJson(rawData, AnswerResponse::class.java)
                commandListener.onUpdateOffer(response)
            }
        }
    }

    private fun onMsgByResponse(rawData: String?, topic: String?) {
        // cần order BE thêm topic để xử lý các case riêng

        Log.d(TAG, "onMsgByResponse: topic= $topic ---  rawData= $rawData")

        try {
            val response = gson.fromJson(rawData, OfferResponse::class.java)
            responseListener.onOffer(response)
        } catch (e: Exception) {
            val response = gson.fromJson(rawData, PeerResponse::class.java)
            val peerBridgeDTO = gson.fromJson(response.data, PeerBridgeMap::class.java)
            Log.d(TAG, "onMsgByResponse models: ${peerBridgeDTO.models}")
            val participantDTOList = gson.fromJson(peerBridgeDTO.models, Array<ParticipantDTO>::class.java).toList()

            val map: HashMap<String, String>? = gson.fromJson(peerBridgeDTO.map, HashMap::class.java) as? HashMap<String, String>

            map?.forEach { (k, v)->
                participantDTOList.forEach {
                    if (v == it.name) {
                        it.streamIdSecondary.add(k)
                    }
                }
            }

            Log.d(TAG, "onMsgByResponse: $participantDTOList")

            responseListener.onPeer(participantDTOList)
        }
    }

    private fun onMsgByEvent(rawData: String?, topic: String?) {
        Log.d(TAG, "onMsgByEvent: topic= $topic ---  rawData= $rawData")

        when (topic) {
            SOCKET_TOPIC.PARTICIPANTS -> {
                val response = gson.fromJson(rawData, ParticipantResponse::class.java)
                eventListener.onParticipantList(response.data ?: mutableListOf())
            }
        }
    }

    private fun onMsgByError(rawData: String?, topic: String?) {
        Log.d(TAG, "onMsgByError: topic= $topic ---  rawData= $rawData")

        val reason = rawData ?: ""
        errorListener.onError(reason)
    }

    fun sendMessageToSocket(rtcDto: RtcBaseRequest, extra: String? = null) {
        try {
            val json = gson.toJson(rtcDto)
//            Log.d(TAG, "send json : type=${rtcDto.type} name=${rtcDto.name} transId=${rtcDto.transId}")
            Log.d(TAG, "\n\nemit:   extra=[$extra]   json=$json")
            webSocket?.send(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
