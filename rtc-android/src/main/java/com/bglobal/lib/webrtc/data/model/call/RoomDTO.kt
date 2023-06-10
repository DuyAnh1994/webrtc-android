package com.bglobal.lib.webrtc.data.model.call

import com.google.gson.annotations.SerializedName

data class RoomDTO(
    @SerializedName("id") var id: Int? = null,

    @SerializedName("participants") var participants: List<ParticipantDTO>? = null
)
