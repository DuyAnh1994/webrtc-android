package com.bglobal.lib.webrtc.data.model.call.offer

import com.bglobal.lib.webrtc.data.model.base.RtcBaseRequest
import com.bglobal.lib.webrtc.data.model.call.DataDTO
import com.google.gson.annotations.SerializedName

data class OfferRequest(
    @SerializedName("data") var dataDto: DataDTO? = null
) : RtcBaseRequest()