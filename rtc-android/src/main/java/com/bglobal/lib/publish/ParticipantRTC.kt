package com.bglobal.lib.publish

import org.webrtc.MediaStream

data class ParticipantRTC(
    var id: Int,
    var name: String,
    var streamIdOrigin: String,
    var streamIdSecondary: MutableList<String>,
    var mediaStream: MediaStream? = null
)
