package com.bglobal.lib.publish

import org.webrtc.MediaStream

interface BglobalRtcListener {
    fun onUserListInRoom(totalList: List<ParticipantRTC>)
    fun onUserJoinRoom(userJoin: ParticipantRTC)
    fun onUserLeaveRoom(userLeave: ParticipantRTC) {}

    fun onAddStream(mediaStream: MediaStream?)
    fun onRemoveStream(mediaStream: MediaStream?)

}
