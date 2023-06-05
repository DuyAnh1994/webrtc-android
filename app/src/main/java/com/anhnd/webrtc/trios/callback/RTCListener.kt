package com.anhnd.webrtc.trios.callback

import org.webrtc.SessionDescription

interface RTCListener {
    fun onSetLocalSdpOffer(state: State, sdp: SessionDescription)
}

enum class State {
    SUCCESS, ERROR
}