package com.anhnd.webrtc.p2p

import com.anhnd.webrtc.p2p.models.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)
}
