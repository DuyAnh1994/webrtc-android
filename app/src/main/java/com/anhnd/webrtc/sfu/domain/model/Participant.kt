package com.anhnd.webrtc.sfu.domain.model

import android.util.Log
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer

data class Participant(
    var id: Int,
    var name: String,
    var streamId: String,
    var mediaStream: MediaStream? = null,
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
