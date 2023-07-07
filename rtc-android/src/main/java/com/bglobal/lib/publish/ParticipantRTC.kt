package com.bglobal.lib.publish

import org.webrtc.MediaStream

data class ParticipantRTC(
    var id: Int? = null,
    var name: String? = null,
    var streamId: String? = null,
    var subIdList: MutableList<String>? = null,
    var mediaStream: MediaStream? = null,
    var isTurnOffCamera: Boolean? = null,
    var isMute: Boolean? = null,
)
