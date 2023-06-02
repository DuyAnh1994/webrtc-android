package com.codewithkael.webrtcprojectforrecord.trios.model.call.request

import com.codewithkael.webrtcprojectforrecord.trios.model.call.response.RoomDto
import com.google.gson.annotations.SerializedName

data class DataDtoRequest(
    @SerializedName("name") var name: String? = null,

    @SerializedName("sdp") var sdp: String? = null
)
