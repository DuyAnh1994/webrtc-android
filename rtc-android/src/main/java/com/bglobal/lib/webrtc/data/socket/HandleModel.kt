package com.bglobal.lib.webrtc.data.socket

import com.bglobal.lib.webrtc.data.model.call.request.DataDtoRequest
import com.bglobal.lib.webrtc.data.model.call.request.RtcDtoRequest

class HandleModel {

    fun createOffer(name: String?, sdp: String): RtcDtoRequest {
        val dataDto = DataDtoRequest(
            name = name,
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
