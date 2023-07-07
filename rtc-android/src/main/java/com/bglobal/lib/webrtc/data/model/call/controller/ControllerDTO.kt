package com.bglobal.lib.webrtc.data.model.call.controller

import com.google.gson.annotations.SerializedName

data class ControllerDTO(
    @SerializedName("name") var name: String? = null,
    @SerializedName("data_channel") var dataChannel: String? = null
)
