package com.bglobal.lib.webrtc.data.model.call.update

import com.google.gson.annotations.SerializedName

data class DataDtoUpdate(

    @SerializedName("sdp") var sdp: String? = null

)
