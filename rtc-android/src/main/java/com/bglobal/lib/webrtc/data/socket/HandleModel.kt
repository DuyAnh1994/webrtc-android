package com.bglobal.lib.webrtc.data.socket

import com.bglobal.lib.webrtc.data.RtcException
import com.bglobal.lib.webrtc.data.model.call.DataDTO
import com.bglobal.lib.webrtc.data.model.call.ParticipantDTO
import com.bglobal.lib.webrtc.data.model.call.answer.AnswerRequest
import com.bglobal.lib.webrtc.data.model.call.offer.OfferRequest
import com.bglobal.lib.webrtc.data.model.call.peer.PeerRequest
import com.google.gson.GsonBuilder

class HandleModel {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    fun createOffer(name: String?, sdp: String): OfferRequest {
        val dataDto = DataDTO(
            name = name,
            sdp = sdp
        )

        return OfferRequest(
            dataDto = dataDto
        ).apply {
            this.type = SOCKET_TYPE.COMMAND
            this.name = SOCKET_TOPIC.JOIN
            this.transId = 0
        }
    }

    fun updateSdp(sdp: String?): AnswerRequest {
        if (sdp == null) {
            throw RtcException("sdp must not null")
        }

        val dataDtoRequest = DataDTO(sdp = sdp)

        return AnswerRequest(
            dataDto = dataDtoRequest
        ).apply {
            this.type = SOCKET_TYPE.RESPONSE
            this.transId = 0
        }
    }

    fun getPeer(name: String, transId: Int = 0): PeerRequest {
        val dataDto = DataDTO(name = name)

        return PeerRequest(dataDto = dataDto).apply {
            this.type = SOCKET_TYPE.COMMAND
            this.name = SOCKET_TOPIC.PEER
            this.transId = transId
        }
    }

    fun getParticipant(rawData: String?): ParticipantDTO? {
        if (rawData == null) return null
        return try {
            gson.fromJson(rawData, ParticipantDTO::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
