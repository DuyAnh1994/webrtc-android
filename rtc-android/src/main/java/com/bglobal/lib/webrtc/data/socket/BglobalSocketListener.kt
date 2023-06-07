package com.bglobal.lib.webrtc.data.socket

import com.bglobal.lib.webrtc.data.model.call.response.RtcDtoResponse
import com.bglobal.lib.webrtc.data.model.event.response.EventDtoResponse
import com.bglobal.lib.webrtc.data.model.call.update.RtcDtoUpdate

interface BglobalSocketListener {
    fun onRtcResponse(rtcDto: RtcDtoResponse) {}
    fun onRtcEvent(eventDto : EventDtoResponse) {}
    fun onRtcUpdate(rtcDto: RtcDtoUpdate) {}
}
