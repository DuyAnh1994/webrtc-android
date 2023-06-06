package com.anhnd.webrtc.utils

import android.app.Activity
import android.view.View
import android.widget.Toast
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun Activity.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}


fun SurfaceViewRenderer.initializeSurfaceView(eglBase: EglBase){
    setEnableHardwareScaler(true)
    setMirror(true)
    init(eglBase.eglBaseContext, null)
}
