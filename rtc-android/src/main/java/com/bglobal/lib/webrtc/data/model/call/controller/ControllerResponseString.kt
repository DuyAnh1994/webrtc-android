package com.bglobal.lib.webrtc.data.model.call.controller

import com.google.gson.annotations.SerializedName

data class ControllerResponseString(
    @SerializedName("data") var data: String? = null
)
