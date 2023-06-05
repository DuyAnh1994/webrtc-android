package com.anhnd.webrtc.trios.model.call.request

import com.google.gson.annotations.SerializedName

data class DataDtoRequest(

    @SerializedName("name") var name: String? = null,

    @SerializedName("sdp") var sdp: String? = null

)
