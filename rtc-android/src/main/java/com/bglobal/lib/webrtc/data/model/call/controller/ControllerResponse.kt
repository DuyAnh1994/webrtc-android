package com.bglobal.lib.webrtc.data.model.call.controller

import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.google.gson.annotations.SerializedName

data class ControllerResponse(
    @SerializedName("data") var controllerDTO: ControllerDTO? = null
) : RtcBaseResponse()
