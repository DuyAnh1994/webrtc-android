package com.bglobal.lib.publish

import org.webrtc.MediaStream

data class ParticipantRTC(
    var id: Int,
    var name: String,
    var streamId: String,
    var subIdList: MutableList<String>,
    var mediaStream: MediaStream? = null
)
