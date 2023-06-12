package com.bglobal.lib.webrtc.data.model.call.peer

import com.google.gson.annotations.SerializedName

data class PeerDTO(
    @SerializedName("map") var map: HashMap<String, String>? = null,

    @SerializedName("instreams") var instreams: List<String>? = null,

    @SerializedName("outstreams") var outstreams: List<String>? = null
)
