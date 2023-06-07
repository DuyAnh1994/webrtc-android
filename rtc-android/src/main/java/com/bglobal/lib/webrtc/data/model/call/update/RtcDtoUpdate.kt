package com.bglobal.lib.webrtc.data.model.call.update

import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.google.gson.annotations.SerializedName

data class RtcDtoUpdate(

    @SerializedName("transId") var transId: Int? = null,

    @SerializedName("data") var dataDto: DataDtoUpdate? = null

) : RtcBaseResponse() {

    fun getSdp(): String? {
        return dataDto?.sdp
    }
}
