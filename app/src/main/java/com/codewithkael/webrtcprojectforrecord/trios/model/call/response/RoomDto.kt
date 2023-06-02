package com.codewithkael.webrtcprojectforrecord.trios.model.call.response

import com.google.gson.annotations.SerializedName

data class RoomDto(
    @SerializedName("id") var id: Int? = null,

    @SerializedName("participants") var participants: List<ParticipantDto>? = null
)
