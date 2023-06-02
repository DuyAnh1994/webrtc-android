package com.codewithkael.webrtcprojectforrecord.trios.model.base

import com.google.gson.annotations.SerializedName

open class RtcBaseResponse {

    @SerializedName("type") var type: String? = null

    @SerializedName("name") var name: String? = null

}
