package com.anhnd.webrtc.sfu.domain.model

import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

data class Participant(
    var id: Int,
    var name: String,
    var streamId: String,
//    var subIdList: MutableList<String> = mutableListOf(),
    var mediaStream: MediaStream? = null,
    var isLoaded: Boolean = false,
    var isLocal: Boolean = false
) {

    fun getVideoTrack(): VideoTrack? {
        return mediaStream?.videoTracks?.firstOrNull()
    }

    fun getStreamIdSecondary(): String {
        val sb = StringBuilder("subIdList: \n")
//        subIdList.forEachIndexed { i, v ->
//            sb.append("$i. ").append(v).append("\n")
//        }
        return sb.toString()
    }


}
