package com.bglobal.lib.webrtc.data.model.call.offer

import com.bglobal.lib.webrtc.data.model.base.RtcBaseResponse
import com.bglobal.lib.webrtc.data.model.call.DataDTO
import com.google.gson.annotations.SerializedName

data class OfferResponse(
    @SerializedName("transId") var transId: Int? = null,

    @SerializedName("data") var dataDto: DataDTO? = null
) : RtcBaseResponse() {

    fun getSdp(): String? {
        return dataDto?.sdp
    }
}