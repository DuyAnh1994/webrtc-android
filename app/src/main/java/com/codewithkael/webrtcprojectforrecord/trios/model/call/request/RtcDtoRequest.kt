package com.codewithkael.webrtcprojectforrecord.trios.model.call.request

import com.google.gson.annotations.SerializedName

data class RtcDtoRequest(
    @SerializedName("type") var type: String? = null,

    @SerializedName("transId") var transId: Int? = null,

    @SerializedName("name") var name: String? = null,

    @SerializedName("data") var dataDto: DataDtoRequest? = null
)
