package com.anhnd.webrtc.trios.model.event.response

import com.anhnd.webrtc.trios.model.base.RtcBaseResponse
import com.anhnd.webrtc.trios.model.call.response.ParticipantDto
import com.google.gson.annotations.SerializedName

data class EventDtoResponse(

    @SerializedName("data") var data: List<ParticipantDto>? = null

) : RtcBaseResponse()
