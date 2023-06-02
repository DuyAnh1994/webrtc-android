package com.codewithkael.webrtcprojectforrecord.trios.model.call.update

import com.codewithkael.webrtcprojectforrecord.trios.model.base.RtcBaseResponse
import com.google.gson.annotations.SerializedName

data class RtcDtoUpdate(
    @SerializedName("transId") var transId: Int? = null,

    @SerializedName("data") var dataDto: DataDtoUpdate? = null
) : RtcBaseResponse()