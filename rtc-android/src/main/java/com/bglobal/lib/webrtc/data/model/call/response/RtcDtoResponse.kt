package com.bglobal.lib.webrtc.data.model.call.response

import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.bglobal.lib.webrtc.data.model.call.response.DataDtoResponse
import com.google.gson.annotations.SerializedName

data class RtcDtoResponse(

    @SerializedName("transId") var transId: Int? = null,

    @SerializedName("data") var dataDto: DataDtoResponse? = null

) : RtcBaseResponse() {

    fun getSdp(): String? {
        return dataDto?.sdp
    }
}
