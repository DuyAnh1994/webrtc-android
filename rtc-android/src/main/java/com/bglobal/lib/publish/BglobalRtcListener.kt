package com.bglobal.lib.publish

import org.webrtc.MediaStream

interface BglobalRtcListener {
    fun onAddStream(mediaStream: MediaStream?)
    fun onRemoveStream(mediaStream: MediaStream?)
}
