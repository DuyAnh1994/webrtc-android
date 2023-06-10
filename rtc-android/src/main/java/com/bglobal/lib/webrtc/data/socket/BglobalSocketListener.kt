package com.bglobal.lib.webrtc.data.socket

import com.bglobal.lib.webrtc.data.model.call.offer.OfferResponse
import com.bglobal.lib.webrtc.data.model.call.ParticipantDTO
import com.bglobal.lib.webrtc.data.model.call.answer.AnswerResponse
import com.bglobal.lib.webrtc.data.model.call.peer.PeerResponse

interface BglobalSocketListener {
    interface Response {
        fun onOffer(response: OfferResponse)
        fun onPeer(response: PeerResponse)
    }

    interface Command {
        fun onAnswer(rtcDto: AnswerResponse)
    }

    interface Event {
        fun onParticipantList(participantList: List<ParticipantDTO>)
    }

    interface Error {
        fun onError(reason: String)
    }
}
