package com.anhnd.webrtc.sfu.domain.model

import android.util.Log
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer

data class Participant(
    var id: Int,
    var name: String,
    var streamIdOrigin: String,
    var streamIdSecondary: MutableList<String> = mutableListOf(),
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

    fun getStreamIdSecondary(): String {
        val sb = StringBuilder()
        streamIdSecondary.forEachIndexed { i, v ->
            sb.append("$i. ").append(v).append("\n")
        }
        return sb.toString()
    }
}
