package com.anhnd.webrtc.trios.domain.model

import org.webrtc.EglBase

data class Participant(
    var id: String,
    var name: String,
    var mediaStreamId: String,
    var eglBase: EglBase? = null
)
