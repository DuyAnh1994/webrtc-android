package com.anhnd.webrtc.trios.model.call.response

import com.google.gson.annotations.SerializedName

data class DataDtoResponse(
    @SerializedName("room") var roomDto: RoomDto? = null,

    @SerializedName("sdp") var sdp: String? = null
)
