package com.bglobal.lib.webrtc.data.model.call.response

import com.bglobal.lib.publish.ParticipantRtcModel
import com.google.gson.annotations.SerializedName

data class ParticipantApiModel(

    @SerializedName("id") var id: Int? = null,

    @SerializedName("name") var name: String? = null,

    @SerializedName("streamsIn") var streamsIn: List<String>? = null,

    @SerializedName("streamsOut") var streamsOut: List<String>? = null,

    @SerializedName("streams") var streams: List<Any>? = null

)

fun ParticipantApiModel.toRtcModel() = ParticipantRtcModel(
    id = this.id ?: 0,
    name = this.name ?: "",
    streamId = streamsIn?.firstOrNull() ?: ""
)
