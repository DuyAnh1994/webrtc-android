package com.bglobal.lib.webrtc.data.model.event.response

import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.bglobal.lib.webrtc.data.model.call.response.ParticipantApiModel
import com.google.gson.annotations.SerializedName

data class EventDtoResponse(

    @SerializedName("data") var data: List<ParticipantApiModel>? = null

) : RtcBaseResponse()
