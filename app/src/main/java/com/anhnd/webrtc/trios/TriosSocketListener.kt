package com.anhnd.webrtc.trios

import com.anhnd.webrtc.trios.model.call.response.RtcDtoResponse
import com.anhnd.webrtc.trios.model.call.update.RtcDtoUpdate
import com.anhnd.webrtc.trios.model.event.response.EventDtoResponse


interface TriosSocketListener {
    fun onRtcResponse(rtcDto: RtcDtoResponse)
    fun onRtcEvent(eventDto : EventDtoResponse)
    fun onRtcUpdate(rtcDto: RtcDtoUpdate)
}
