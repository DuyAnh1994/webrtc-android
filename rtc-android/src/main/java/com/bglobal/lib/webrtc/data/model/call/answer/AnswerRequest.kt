package com.bglobal.lib.webrtc.data.model.call.answer

import com.bglobal.lib.webrtc.data.model.base.RtcBaseRequest
import com.bglobal.lib.webrtc.data.model.call.DataDTO
import com.google.gson.annotations.SerializedName

data class AnswerRequest(
    @SerializedName("data") var dataDto: DataDTO? = null
) : RtcBaseRequest()
