package com.bglobal.lib.webrtc.data.model.call

import com.bglobal.lib.publish.ParticipantRTC
import com.google.gson.annotations.SerializedName

data class ParticipantDTO(
    @SerializedName("id") var id: Int? = null,

    @SerializedName("name") var name: String? = null,

    @SerializedName("streamId") var streamId: String? = null,

    var subIdList: MutableList<String> = mutableListOf()
)

fun ParticipantDTO.toParticipantRTC() = ParticipantRTC(
    id = this.id ?: 0,
    name = this.name ?: "",
    streamId = streamId ?: "",
    subIdList = subIdList
)
