package com.bglobal.lib.webrtc.data.model.call.answer

import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.google.gson.annotations.SerializedName

data class AnswerResponse(
    @SerializedName("data") var dataDto: AnswerDTO? = null
) : RtcBaseResponse() {

    fun getSdp(): String? {
        return dataDto?.sdp
    }
}
