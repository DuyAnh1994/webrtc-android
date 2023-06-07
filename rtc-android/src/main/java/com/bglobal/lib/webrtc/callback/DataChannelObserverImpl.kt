package com.bglobal.lib.webrtc.callback

import org.webrtc.DataChannel

abstract class DataChannelObserverImpl : DataChannel.Observer {
    override fun onBufferedAmountChange(p0: Long) {}
    override fun onStateChange() {}
    override fun onMessage(p0: DataChannel.Buffer?) {}
}
