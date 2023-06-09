package com.bglobal.lib.webrtc.data.model.call.response

import com.google.gson.annotations.SerializedName

data class RoomDto(

    @SerializedName("id") var id: Int? = null,

    @SerializedName("participants") var participants: List<ParticipantApiModel>? = null

)
