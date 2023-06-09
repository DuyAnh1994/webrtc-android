package com.bglobal.lib.publish

import org.webrtc.MediaStream

interface BglobalRtcListener {
    fun onUserListInRoom(totalList: List<ParticipantRtcModel>)
    fun onUserJoinRoom(userJoin: ParticipantRtcModel)
    fun onUserLeaveRoom(userLeave: ParticipantRtcModel) {}

    fun onAddStream(mediaStream: MediaStream?)
    fun onRemoveStream(mediaStream: MediaStream?)

}
