package com.bglobal.lib.webrtc.data.model.call.peer

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

data class PeerBridgeMap(
    @SerializedName("models") var models: String? = null,

    @SerializedName("map") var map: String? = null,
//
//    @SerializedName("instreams") var instreams: List<String>? = null,
//
//    @SerializedName("outstreams") var outstreams: List<String>? = null
)

fun PeerBridgeMap.toDTO(): PeerDTO {
    val hashMap: HashMap<String, String>? = GsonBuilder().disableHtmlEscaping().create().fromJson(map, HashMap::class.java) as? HashMap<String, String>
    return PeerDTO(
        map = hashMap
    )
}
