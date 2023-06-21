package com.bglobal.lib.utils

//const val TAG = "webrtcP2P"
const val TAG = "webrtcSfu"
const val TAG_SOCKET = "webrtc_socket"
//const val TAG = "webrtcMCU"

fun <T> MutableList<T>.replace(list: List<T>) {
    clear()
    addAll(list)
}
