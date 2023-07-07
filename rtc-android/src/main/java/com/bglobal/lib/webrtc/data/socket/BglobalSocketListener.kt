package com.bglobal.lib.webrtc.data.socket

import com.bglobal.lib.webrtc.data.model.call.ParticipantDTO
import com.bglobal.lib.webrtc.data.model.call.answer.AnswerResponse
import com.bglobal.lib.webrtc.data.model.call.controller.ControllerDTO
import com.bglobal.lib.webrtc.data.model.call.offer.OfferResponse
import com.bglobal.lib.webrtc.data.model.call.option.OptionDTO
import org.java_websocket.handshake.ServerHandshake

interface BglobalSocketListener {
    interface Common {
        fun onOpen(url: String, handshakedata: ServerHandshake?)
        fun onClose(code: Int, reason: String?, remote: Boolean)
        fun onError(ex: Exception?)
    }

    interface Response {
        fun onOffer(response: OfferResponse)
        fun onPeer(participantDTOList: List<ParticipantDTO>)
    }

    interface Command {
        fun onUpdateOffer(rtcDto: AnswerResponse)
    }

    interface Event {
        fun onParticipantList(participantList: List<ParticipantDTO>) {}
        fun onDataChannel(controllerDTO: ControllerDTO) {}
        fun onOption(name: String?, option: OptionDTO)
    }

    interface Error {
        fun onError(reason: String)
    }
}
