package com.anhnd.webrtc.trios.display

import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer

data class StreamDisplay(
    var id: String? = null,
    var mediaStream: MediaStream? = null,
    val surfaceViewRenderer: SurfaceViewRenderer? = null
) {

    fun isSameMediaStream(id: String): Boolean {
        return mediaStream?.id == id
    }
}
