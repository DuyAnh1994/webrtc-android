package com.bglobal.lib.publish

import android.app.Application
import android.util.Log
import com.bglobal.lib.webrtc.callback.PeerConnectionObserverImpl
import com.bglobal.lib.webrtc.data.RtcException
import com.bglobal.lib.webrtc.data.model.call.response.RtcDtoResponse
import com.bglobal.lib.webrtc.data.model.call.update.RtcDtoUpdate
import com.bglobal.lib.webrtc.data.socket.BglobalSocketClient
import com.bglobal.lib.webrtc.data.socket.BglobalSocketListener
import com.bglobal.lib.webrtc.data.socket.HandleModel
import com.bglobal.lib.webrtc.RTCAudioManager
import com.bglobal.lib.webrtc.BglobalRtcClient
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class RtcManager(private val application: Application) {


    companion object {
        private const val TAG = "RtcManager"
    }
    private var socket: BglobalSocketClient? = null
    private val socketListener = object : BglobalSocketListener {
        override fun onRtcResponse(rtcDto: RtcDtoResponse) {
            offerResponse(rtcDto.getSdp())
            Log.d(TAG, "onRtcResponse: ${rtcDto}")
        }

        override fun onRtcUpdate(rtcDto: RtcDtoUpdate) {
            createAnswer(rtcDto.getSdp())
        }
    }

    private var rtcClient: BglobalRtcClient? = null
    private var rtcListener: BglobalRtcListener? = null

    private val handleModel by lazy { HandleModel() }
    private val rtcAudioManager by lazy { RTCAudioManager.create(application.applicationContext) }


    init {

    }

    fun build() {
        socket = BglobalSocketClient(socketListener)
        rtcClient = BglobalRtcClient(
            application = this.application,
            observer = peerConnectionObserverImpl,
            callback = callback
        )
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
    }

    fun createOffer() {
        rtcClient?.createOffer()
    }

    private fun onCreateOffer(name: String? = null, sdp: String?) {
        if (sdp == null) {
            throw RtcException("sdp must not null")
        }
        val rtcDto = handleModel.createOffer(name = name, sdp = sdp)
        socket?.sendMessageToSocket(rtcDto)
    }

    private fun offerResponse(sdp: String?) {
        /*
         * set remote với type == ANSWER để stream lên server
         */
        val session = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        rtcClient?.setRemoteSdpByAnswer(session)
    }

    fun createAnswer(sdp: String?) {
        val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
        rtcClient?.setRemoteSdpByOffer(offer)
        rtcClient?.createAnswer {
            val rtcDto = handleModel.update(it?.description)
            socket?.sendMessageToSocket(rtcDto)
        }
    }

    fun getEglBase(): EglBase {
        return rtcClient?.eglBase ?: throw RtcException("EglBase instance not yet initialization")
    }

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        Log.d(TAG, "onBind: ccccccc")
        rtcClient?.startLocalVideo(surface)
    }

    fun addRtcListener(listener: BglobalRtcListener) {
        try {
            rtcListener = listener
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeRctListener() {
        rtcListener = null
    }



    private val callback = object : BglobalRtcClient.Callback {
        override fun onSetLocalSdpOffer(state: BglobalRtcClient.State, sdp: SessionDescription) {
            onCreateOffer(sdp = sdp.description)
        }
    }

    private val peerConnectionObserverImpl = object : PeerConnectionObserverImpl() {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            rtcClient?.addIceCandidate(iceCandidate)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            rtcListener?.onAddStream(mediaStream)
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
            rtcListener?.onRemoveStream(mediaStream)
        }
    }
}
