package com.bglobal.lib.webrtc.data.model.call.answer

import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.bglobal.lib.webrtc.data.model.call.DataDTO
import com.google.gson.annotations.SerializedName

data class AnswerResponse(
    @SerializedName("data") var dataDto: DataDTO? = null
) : RtcBaseResponse() {

    fun getSdp(): String? {
        return dataDto?.sdp
    }
}
