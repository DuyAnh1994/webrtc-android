package com.anhnd.webrtc.trios.callback

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

abstract class SdpObserverImpl : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}
