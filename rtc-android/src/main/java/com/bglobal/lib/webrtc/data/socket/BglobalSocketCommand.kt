package com.bglobal.lib.webrtc.data.socket

typealias SOCKET_TYPE = BglobalSocketCommand.TYPE
typealias SOCKET_TOPIC = BglobalSocketCommand.TOPIC

object BglobalSocketCommand {
    object TYPE {
        const val COMMAND = "cmd"
        const val RESPONSE = "response"
        const val EVENT = "event"
        const val ERROR = "error"
    }

    object TOPIC {
        const val JOIN = "join"
        const val UPDATE = "update"
        const val PARTICIPANTS = "participants"
        const val PEER = "peer"
    }
}
