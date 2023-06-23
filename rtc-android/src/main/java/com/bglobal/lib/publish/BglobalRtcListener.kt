package com.bglobal.lib.publish

import org.webrtc.MediaStream

interface BglobalRtcListener {
    fun onConnect(url: String, code: Int, msg: String)
    fun onDisconnect(code: Int, reason: String?, remote: Boolean) {}
    fun onError(exception: Exception?) {}
    fun onUserListInRoom(totalList: List<ParticipantRTC>)
    fun onUserJoinRoom(user: ParticipantRTC) {}
    fun onUserLeaveRoom(user: ParticipantRTC) {}

    fun onAddStream(track: MediaStream?) {}
    fun onRemoveStream(track: MediaStream?) {}

    fun onCameraSwitchDone(frontCamera: Boolean) {}
    fun onCameraSwitchError(reason: String) {}
}
