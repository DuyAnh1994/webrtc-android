package com.anhnd.webrtc.trios

import com.anhnd.webrtc.trios.model.call.request.DataDtoRequest
import com.anhnd.webrtc.trios.model.call.request.RtcDtoRequest

class HandleModel {

    fun createOffer(sdp: String): RtcDtoRequest {
        val dataDto = DataDtoRequest(
            name = "user_from_app",
            sdp = sdp
        )

        val rtcDto = RtcDtoRequest(
            type = "cmd",
            transId = 0,
            name = "join",
            dataDto = dataDto
        )

        return rtcDto
    }


    fun update(sdp: String?): RtcDtoRequest {
        val dataDtoRequest = DataDtoRequest(sdp = sdp)

        val rtcDto = RtcDtoRequest(
            type = "response",
            transId = 0,
            dataDto = dataDtoRequest
        )

        return rtcDto
    }
}
