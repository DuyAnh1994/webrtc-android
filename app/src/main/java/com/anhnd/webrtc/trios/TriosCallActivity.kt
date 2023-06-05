package com.anhnd.webrtc.trios

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.ActivityTriosCallBinding
import com.anhnd.webrtc.trios.model.call.request.DataDtoRequest
import com.anhnd.webrtc.trios.model.call.request.RtcDtoRequest
import com.anhnd.webrtc.trios.model.call.response.RtcDtoResponse
import com.anhnd.webrtc.trios.model.call.update.RtcDtoUpdate
import com.anhnd.webrtc.trios.model.event.response.EventDtoResponse
import com.anhnd.webrtc.utils.RTCAudioManager
import com.anhnd.webrtc.utils.gone
import com.anhnd.webrtc.utils.show
import com.permissionx.guolindev.PermissionX
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription

class TriosCallActivity : AppCompatActivity(), TriosSocketListener {

    companion object {
        private const val TAG = "TriosCallActivity"
    }

    lateinit var binding: ActivityTriosCallBinding
    private var socketClient: TriosSocket? = null
    private var rtcClient: TriosRTCClient? = null
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private val streamList = mutableListOf<MediaStream?>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTriosCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ).request { allGranted, _, _ ->
                if (allGranted) {
                    socketClient = TriosSocket(this)
                    rtcClient = TriosRTCClient(
                        application = application,
                        socket = socketClient!!,
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

    override fun onRtcResponse(rtcDto: RtcDtoResponse) {
        Log.d(TAG, "onRtcResponse() called with: rtcDto = ${rtcDto.type}")
        offerResponse(rtcDto.dataDto?.sdp)
    }

    override fun onRtcEvent(eventDto: EventDtoResponse) {
        Log.d(TAG, "onRtcEvent() called with: eventDto = ${eventDto.type}")
    }

    override fun onRtcUpdate(rtcDto: RtcDtoUpdate) {
        Log.d(TAG, "onRtcUpdate() called with: eventDto = ${rtcDto.type}")
        update(rtcDto)
    }

    private fun update(rtcDto: RtcDtoUpdate) {
        val offer = SessionDescription(SessionDescription.Type.OFFER, rtcDto.dataDto?.sdp)
        rtcClient?.setRemoteDesc(offer)
        rtcClient?.createAnswer {
            val request = RtcDtoRequest(
                type = "response",
                transId = 0,
                dataDto = DataDtoRequest(sdp = it?.description)
            )
            socketClient?.sendMessageToSocket(request)
        }
    }

    private fun callRequest() {
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
                rtcClient?.createOffer(targetUserNameEt.text.toString())
            }
        }
    }

    private fun offerResponse(sdp: String?) {
        Log.d(TAG, "offerResponse: ccccccccc")
        runOnUiThread {
            val session = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            rtcClient?.setRemoteDesc(session)


//            rtcClient?.createAnswer() {
//                hideLoading()
//            }
        }
    }

    private fun answerResponse() {
        val sdp = "sdp..."
//        Log.d(TAG, "sdp: $sdp")

        val session = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        rtcClient?.setRemoteDesc(session)
        runOnUiThread {
            hideLoading()
        }
    }

    private fun setCallLayoutVisible() {
        binding.callLayout.show()
    }

    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.gone()
    }

    private fun setWhoToCallLayoutVisible() {
        binding.whoToCallLayout.show()
    }

    private fun showLoading() {
        binding.remoteViewLoading.show()
    }

    private fun hideLoading() {
        binding.remoteViewLoading.gone()
    }


    private val peerConnectionObserverImpl = object : PeerConnection.Observer {
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            Log.d(TAG, "onSignalingChange() called with: p0 = ${p0?.name}")
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            Log.d(TAG, "onIceConnectionChange() called with: p0 = ${p0?.name}")
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

        override fun onAddStream(p0: MediaStream?) {
            Log.d(TAG, "onAddStream() called with: p0 = ${p0?.id}")
            p0?.videoTracks?.forEachIndexed { i, v ->
                when (i) {
                    0 -> v.addSink(binding.svr0)
                    1 -> v.addSink(binding.svr1)
                    2 -> v.addSink(binding.svr2)
                    else -> {}
                }
            }
//            p0?.videoTracks?.get(0)?.addSink(binding.svr1)

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

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
            Log.d(TAG, "onAddTrack() called with: p0 = $p0, p1 = ${p1?.count()}")
        }
    }
}
