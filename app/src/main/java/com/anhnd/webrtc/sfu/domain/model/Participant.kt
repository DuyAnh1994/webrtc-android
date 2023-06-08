package com.anhnd.webrtc.sfu.domain.model

import android.util.Log
import android.view.TextureView
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame.TextureBuffer

data class Participant(
    var index: Int,
    var id: String? = null,
    var mediaStream: MediaStream? = null,
    var name: String = "",
    var isLoaded: Boolean = false,
    var isLocal: Boolean = false
) {

    fun addSink(surface: SurfaceViewRenderer) {
//        if (!isLoaded) {
            mediaStream?.videoTracks?.firstOrNull()?.addSink(surface)
        Log.d("anhnd", "addSink: ${mediaStream?.videoTracks?.firstOrNull()}")
            isLoaded = true
//        }
    }
}
