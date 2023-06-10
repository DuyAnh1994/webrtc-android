package com.bglobal.lib.webrtc.data.model.call.peer

import com.google.gson.annotations.SerializedName

data class PeerResponse(
    @SerializedName("data") var data: String? = null
)
