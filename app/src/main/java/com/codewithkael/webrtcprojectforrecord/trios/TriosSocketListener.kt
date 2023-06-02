package com.codewithkael.webrtcprojectforrecord.trios

import com.codewithkael.webrtcprojectforrecord.trios.model.call.response.RtcDtoResponse
import com.codewithkael.webrtcprojectforrecord.trios.model.call.update.RtcDtoUpdate
import com.codewithkael.webrtcprojectforrecord.trios.model.event.response.EventDtoResponse

interface TriosSocketListener {
    fun onRtcResponse(rtcDto: RtcDtoResponse)
    fun onRtcEvent(eventDto : EventDtoResponse)
    fun onRtcUpdate(rtcDto: RtcDtoUpdate)
}
