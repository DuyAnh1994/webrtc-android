package com.bglobal.lib.webrtc.data.model.call.participant

import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.bglobal.lib.webrtc.data.model.call.ParticipantDTO
import com.google.gson.annotations.SerializedName

data class ParticipantResponse(
    @SerializedName("data") var data: List<ParticipantDTO>? = null
) : RtcBaseResponse()
