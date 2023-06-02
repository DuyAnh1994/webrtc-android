package com.codewithkael.webrtcprojectforrecord.trios.model.call.response

import com.google.gson.annotations.SerializedName

data class ParticipantDto(

    @SerializedName("id") var id: Int? = null,

    @SerializedName("name") var name: String? = null,

    @SerializedName("streams") var streams: List<Any>? = null

)
