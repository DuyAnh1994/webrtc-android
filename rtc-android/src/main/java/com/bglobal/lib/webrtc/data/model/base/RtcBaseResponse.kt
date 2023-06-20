package com.bglobal.lib.webrtc.data.model.base

import com.google.gson.annotations.SerializedName

open class RtcBaseResponse(
    @SerializedName("type") var type: String? = null,

    @SerializedName("name") var topic: String? = null,

    @SerializedName("transId") var transId: Int? = null
)
