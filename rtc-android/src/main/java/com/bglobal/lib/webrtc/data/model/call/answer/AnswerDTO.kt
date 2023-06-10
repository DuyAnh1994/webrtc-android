package com.bglobal.lib.webrtc.data.model.call.answer

import com.google.gson.annotations.SerializedName

data class AnswerDTO(
    @SerializedName("sdp") var sdp: String? = null
)
