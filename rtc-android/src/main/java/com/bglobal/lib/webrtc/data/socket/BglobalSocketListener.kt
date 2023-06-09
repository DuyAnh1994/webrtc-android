package com.bglobal.lib.webrtc.data.socket

import com.bglobal.lib.webrtc.data.model.call.response.ParticipantApiModel
import com.bglobal.lib.webrtc.data.model.call.response.RtcDtoResponse
import com.bglobal.lib.webrtc.data.model.call.update.RtcDtoUpdate

interface BglobalSocketListener {
    interface Response {
        fun onRtcResponse(rtcDto: RtcDtoResponse)
    }

    interface Update {
        fun onRtcUpdate(rtcDto: RtcDtoUpdate)
    }

    interface Event {
        fun onParticipantList(participantList: List<ParticipantApiModel>)
    }
}
