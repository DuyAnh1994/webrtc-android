package com.bglobal.lib.publish

import org.webrtc.MediaStream

data class StreamAsync(
    var id : String?,
    var track : MediaStream?,
    var type  : RoomState
)
