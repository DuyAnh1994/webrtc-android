package com.bglobal.lib.webrtc.data.model.call

import com.google.gson.annotations.SerializedName

data class DataDTO(
    @SerializedName("sdp") var sdp: String? = null,

    @SerializedName("name") var name: String? = null,

    @SerializedName("room") var roomDto: RoomDTO? = null
)
