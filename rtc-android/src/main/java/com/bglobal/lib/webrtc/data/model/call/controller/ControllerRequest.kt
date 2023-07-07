package com.bglobal.lib.webrtc.data.model.call.controller

import com.bglobal.lib.webrtc.data.model.base.RtcBaseRequest
import com.google.gson.annotations.SerializedName

data class ControllerRequest(
    @SerializedName("data") var controllerDTO: ControllerDTO? = null
) : RtcBaseRequest()
