package com.codewithkael.webrtcprojectforrecord.trios.model.event.response

import com.codewithkael.webrtcprojectforrecord.trios.model.base.RtcBaseResponse
import com.codewithkael.webrtcprojectforrecord.trios.model.call.response.ParticipantDto
import com.google.gson.annotations.SerializedName

data class EventDtoResponse(

    @SerializedName("data") var data: List<ParticipantDto>? = null

) : RtcBaseResponse()
