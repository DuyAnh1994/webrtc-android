package com.anhnd.webrtc.trios

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.SfuActivityBinding
import com.anhnd.webrtc.trios.callback.RTCListener
import com.anhnd.webrtc.trios.callback.State
import com.anhnd.webrtc.trios.model.call.response.RtcDtoResponse
import com.anhnd.webrtc.trios.model.call.update.RtcDtoUpdate
import com.anhnd.webrtc.trios.model.event.response.EventDtoResponse
import com.anhnd.webrtc.utils.PeerConnectionObserverImpl
import com.anhnd.webrtc.utils.RTCAudioManager
import com.anhnd.webrtc.utils.TAG
import com.anhnd.webrtc.utils.gone
import com.anhnd.webrtc.utils.show
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription

class SfuActivity : AppCompatActivity() {

    private lateinit var binding: SfuActivityBinding
    private var rtcClient: TriosRTCClient? = null
    private var socket: TriosSocket? = null
    private val handleModel by lazy { HandleModel() }
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private val userMap = mutableMapOf<String?, MediaStream?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SfuActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        socket = TriosSocket(socketListener)
        rtcClient = TriosRTCClient(
            application = application,
            observer = peerConnectionObserverImpl,
            listener = rtcListener
        )
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)


        binding.sendCmd.setOnClickListener { doCall() }
    }

    private fun doCall() {
        runOnUiThread {
            setWhoToCallLayoutGone()
            setCallLayoutVisible()
            binding.apply {
                rtcClient?.initializeSurfaceView(svrMyCamera)
                rtcClient?.initializeSurfaceView(svrRemote0)
                rtcClient?.initializeSurfaceView(svrRemote1)
                rtcClient?.initializeSurfaceView(svrRemote2)
                rtcClient?.startLocalVideo(svrMyCamera)
                rtcClient?.createOffer()
            }
        }
    }

    private fun createOffer(sdp: SessionDescription) {
        val rtcDto = handleModel.createOffer(sdp.description)
        socket?.sendMessageToSocket(rtcDto)
    }

    private fun offerResponse(sdp: String?) {
        runOnUiThread {
            /**
             * set remote với type == ANSWER để stream lên server
             */
            val session = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            rtcClient?.setRemoteSdpByAnswer(session)
        }
    }

    private fun update(sdp: String?) {
        val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
        rtcClient?.setRemoteSdpByOffer(offer)
        rtcClient?.createAnswer {
            val rtcDto = handleModel.update(it?.description)
            socket?.sendMessageToSocket(rtcDto)
        }
    }

    private fun setCallLayoutVisible() {
        binding.callLayout.show()
    }

    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.gone()
    }

    private fun showLoading() {
        binding.remoteViewLoading.show()
    }

    private fun hideLoading() {
        binding.remoteViewLoading.gone()
    }


    /**
     * socket
     */
    private val socketListener = object : TriosSocketListener {
        override fun onRtcResponse(rtcDto: RtcDtoResponse) {
            offerResponse(rtcDto.getSdp())
        }

        override fun onRtcUpdate(rtcDto: RtcDtoUpdate) {
            /*
             * - test chưa thấy cần sử dụng func vẫn có thể stream từ app
             * - vẫn tồn tại trong flow của web
             */
            update(rtcDto.getSdp())
        }

        override fun onRtcEvent(eventDto: EventDtoResponse) {
//            rtcClient?.getSenderList()
        }
    }


    /**
     * peer connection
     */

    private val rtcListener = object : RTCListener {
        override fun onSetLocalSdpOffer(state: State, sdp: SessionDescription) {
            createOffer(sdp)
        }
    }

    private val peerConnectionObserverImpl = object : PeerConnectionObserverImpl() {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            rtcClient?.addIceCandidate(iceCandidate)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            /**
             * có stream mới trong luồng
             */
            Log.d(TAG, "onAddStream() called with: mediaStream = ${mediaStream?.id}")
            if (!userMap.containsKey(mediaStream?.id)) {
                userMap[mediaStream?.id] = mediaStream
            }

            userMap.keys.forEachIndexed { i, _ ->
                when (i) {
                    0 -> mediaStream?.videoTracks?.firstOrNull()?.addSink(binding.svrRemote0)
                    1 -> mediaStream?.videoTracks?.firstOrNull()?.addSink(binding.svrRemote1)
                    2 -> mediaStream?.videoTracks?.firstOrNull()?.addSink(binding.svrRemote2)
                    else -> {}
                }
            }
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
            Log.d(TAG, "onRemoveStream() called with: p0 = ${mediaStream?.id}")
        }
    }
}
