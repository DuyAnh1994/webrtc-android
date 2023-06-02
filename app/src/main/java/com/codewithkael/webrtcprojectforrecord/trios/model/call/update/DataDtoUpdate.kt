package com.codewithkael.webrtcprojectforrecord.trios.model.call.update

import com.google.gson.annotations.SerializedName

data class DataDtoUpdate(
    @SerializedName("sdp") var sdp: String? = null
)
