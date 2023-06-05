package com.anhnd.webrtc.trios

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.SfuActivityBinding
import com.anhnd.webrtc.trios.callback.RTCListener
import com.anhnd.webrtc.trios.callback.State
import com.anhnd.webrtc.trios.model.call.request.DataDtoRequest
import com.anhnd.webrtc.trios.model.call.request.RtcDtoRequest
import com.anhnd.webrtc.trios.model.call.response.RtcDtoResponse
import com.anhnd.webrtc.trios.model.call.update.RtcDtoUpdate
import com.anhnd.webrtc.trios.model.event.response.EventDtoResponse
import com.anhnd.webrtc.utils.PeerConnectionObserverImpl
import com.anhnd.webrtc.utils.RTCAudioManager
import com.anhnd.webrtc.utils.TAG
import com.anhnd.webrtc.utils.gone
import com.anhnd.webrtc.utils.show
import com.google.gson.GsonBuilder
import com.permissionx.guolindev.PermissionX
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
    private val streamList = mutableListOf<MediaStream?>()
    private val gson = GsonBuilder().disableHtmlEscaping().create();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SfuActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ).request { allGranted, _, _ ->
                if (allGranted) {
                    socket = TriosSocket(socketListener)
                    rtcClient = TriosRTCClient(
                        application = application,
                        observer = peerConnectionObserverImpl
                    )
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
                } else {
                    Toast.makeText(this, "you should accept all permissions", Toast.LENGTH_LONG).show()
                }
            }

        binding.sendCmd.setOnClickListener {
            callRequest()
        }
    }

    private fun update(rtcDto: RtcDtoUpdate) {
        val offer = SessionDescription(SessionDescription.Type.OFFER, rtcDto.getSdp())
        rtcClient?.setRemoteAnswer(offer)
        rtcClient?.createAnswer {
            val request = RtcDtoRequest(
                type = "response",
                transId = 0,
                dataDto = DataDtoRequest(sdp = rtcDto.getSdp())
            )
            socket?.sendMessageToSocket(request)
        }
    }

    private fun callRequest() {
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
                rtcClient?.initializeSurfaceView(svr0)
                rtcClient?.initializeSurfaceView(svr1)
                rtcClient?.initializeSurfaceView(svr2)
                rtcClient?.startLocalVideo(svrMyCamera)
//                rtcClient?.createDataChannel("room 1")
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
            val session = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            rtcClient?.setRemoteAnswer(session)
//            rtcClient?.createAnswer() {
//                hideLoading()
//            }
        }
    }

    private fun update(sdp: String?) {
        val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
        rtcClient?.setRemoteAnswer(offer)
        rtcClient?.createAnswer {
            val rtcDto = handleModel.update(sdp)
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
            update(rtcDto.dataDto?.sdp)
        }

        override fun onRtcEvent(eventDto: EventDtoResponse) {}
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
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            Log.d(TAG, "onSignalingChange() called with: p0 = ${p0?.name}")
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            /**
             * xử lý khi có new user kết nối tới SFU
             */

            Log.d(TAG, "onIceConnectionChange() called with: p0 = ${p0?.name}")
            if (p0 == PeerConnection.IceConnectionState.CONNECTED) {
                // coding
            }
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            Log.d(TAG, "onIceConnectionReceivingChange() called with: p0 = $p0")
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            Log.d(TAG, "onIceGatheringChange() called with: p0 = ${p0?.name}")
        }

        override fun onIceCandidate(p0: IceCandidate?) {
//            Log.d(TAG, "onIceCandidate() called with: p0 = $p0")

            rtcClient?.addIceCandidate(p0)

//                val candidate = hashMapOf(
//                    "sdpMid" to p0?.sdpMid,
//                    "sdpMLineIndex" to p0?.sdpMLineIndex,
//                    "sdpCandidate" to p0?.sdp
//                )

//                socketRepository?.sendMessageToSocket(
//                    MessageModel("ice_candidate", userName, target, candidate)
//                )
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
            Log.d(TAG, "onIceCandidatesRemoved() called with: p0 = ${p0?.count()}")
        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
            Log.d(TAG, "onAddTrack() called with: p0 = $p0, p1 = ${p1?.count()}")
        }

        override fun onAddStream(p0: MediaStream?) {
            /**
             * có stream mới trong luồng
             */
            Log.d(TAG, "onAddStream() called with: p0 = ${p0?.id}")

//            p0?.videoTracks?.forEachIndexed { i, v ->
//                when (i) {
//                    0 -> v.addSink(binding.svr0)
//                    1 -> v.addSink(binding.svr1)
//                    2 -> v.addSink(binding.svr2)
//                    else -> {}
//                }
//            }

            p0?.videoTracks?.get(0)?.addSink(binding.svr1)

//            streamList.add(p0)
//            streamList.forEachIndexed { i, v ->
//                when (i) {
//                    0 -> v?.videoTracks?.get(0)?.addSink(binding.svr1)
//                    1 -> v?.videoTracks?.get(0)?.addSink(binding.svr2)
//                    2 -> v?.videoTracks?.get(0)?.addSink(binding.svr3)
//                    else -> {}
//                }
//            }
        }

        override fun onRemoveStream(p0: MediaStream?) {
            Log.d(TAG, "onRemoveStream() called with: p0 = ${p0?.id}")
        }

        override fun onDataChannel(p0: DataChannel?) {
            Log.d(TAG, "onDataChannel() called with: p0 = $p0")
        }

        override fun onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded() called")
        }
    }
}
