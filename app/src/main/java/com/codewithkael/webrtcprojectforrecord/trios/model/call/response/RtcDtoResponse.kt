package com.codewithkael.webrtcprojectforrecord.trios.model.call.response

import com.codewithkael.webrtcprojectforrecord.trios.model.base.RtcBaseResponse
import com.google.gson.annotations.SerializedName

data class RtcDtoResponse(

    @SerializedName("transId") var transId: Int? = null,

    @SerializedName("data") var dataDto: DataDtoResponse? = null

) : RtcBaseResponse()
