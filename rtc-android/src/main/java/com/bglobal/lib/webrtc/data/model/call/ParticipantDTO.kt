package com.bglobal.lib.webrtc.data.model.call

import com.bglobal.lib.publish.ParticipantRTC
import com.google.gson.annotations.SerializedName

data class ParticipantDTO(
    @SerializedName("id") var id: Int? = null,

    @SerializedName("name") var name: String? = null,

    @SerializedName("streamsIn") var streamsIn: List<String>? = null,

    @SerializedName("streamsOut") var streamsOut: List<String>? = null,

//    @SerializedName("streamMap") var streamMap: List<String>? = null, // server mới define, chưa cần sử dụng

//    @SerializedName("streams") var streams: List<Any>? = null // luôn trả về null, k cần thiết sử dụng

    var streamId: String? = null
)

fun ParticipantDTO.toParticipantRTC() = ParticipantRTC(
    id = this.id ?: 0,
    name = this.name ?: "",
    streamId = streamsIn?.firstOrNull() ?: ""
)
