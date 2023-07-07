package com.bglobal.lib.webrtc.data.model.call.option

import com.google.gson.annotations.SerializedName

data class OptionDTO(
    @SerializedName("is_turn_off_camera")
    var isTurnOffCamera: Boolean? = null,

    @SerializedName("is_mute")
    var mute: Boolean? = null
)
