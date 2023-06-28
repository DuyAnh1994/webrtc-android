package com.anhnd.webrtc.utils

import android.app.Activity
import android.view.View
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.flow.MutableStateFlow
import org.webrtc.EglBase
import org.webrtc.MediaStreamTrack
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import kotlin.math.max
import kotlin.math.min

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


fun SurfaceViewRenderer.initializeSurfaceView(eglBase: EglBase) {
    setEnableHardwareScaler(true)
    setMirror(true)
    init(eglBase.eglBaseContext, null)
}

fun <T> MutableLiveData<T>.asLiveData() = this as LiveData<T>

fun <E> LiveData<List<E>>.isEmptyList() = value.isNullOrEmpty()

fun <T> MutableLiveData<T>.postSelf() {
    postValue(this.value)
}

fun <T> MutableLiveData<T>.setSelf() {
    value = this.value
}

fun <T> MutableStateFlow<T>.postSelf() {
    value = this.value
}

fun <T> AppCompatActivity.observer(liveData: LiveData<T>, onChange: (T) -> Unit) {
    liveData.observe(this, Observer(onChange))
}

fun <T> MutableList<T>.swap(index1: Int, index2: Int){
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}

fun SurfaceViewRenderer.addSink(track : VideoTrack?) {
    track?.addSink(this)
}

fun SurfaceViewRenderer.removeSink(track : VideoTrack?) {
    track?.removeSink(this)
}
