package com.codewithkael.webrtcprojectforrecord.trios

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.codewithkael.webrtcprojectforrecord.databinding.ActivityTriosCallBinding
import com.codewithkael.webrtcprojectforrecord.trios.model.call.request.DataDtoRequest
import com.codewithkael.webrtcprojectforrecord.trios.model.call.request.RtcDtoRequest
import com.codewithkael.webrtcprojectforrecord.trios.model.call.response.RtcDtoResponse
import com.codewithkael.webrtcprojectforrecord.trios.model.call.update.RtcDtoUpdate
import com.codewithkael.webrtcprojectforrecord.trios.model.event.response.EventDtoResponse
import com.codewithkael.webrtcprojectforrecord.utils.PeerConnectionObserver
import com.codewithkael.webrtcprojectforrecord.utils.RTCAudioManager
import com.codewithkael.webrtcprojectforrecord.utils.gone
import com.codewithkael.webrtcprojectforrecord.utils.show
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTriosCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val socketIO = TriosSocketIO()
        socketClient = TriosSocket(this)
        rtcClient = TriosRTCClient(
            application = application,
            socket = socketClient!!,
            observer = peerConnectionObserverImpl
        )

        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
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
        val dataDtoRequest = DataDtoRequest(sdp = rtcDto.dataDto?.sdp)
        val request = RtcDtoRequest(
            type = "response",
            transId = 0,
            dataDto = dataDtoRequest
        )
        socketClient?.sendMessageToSocket(request)
    }

    private fun callRequest() {
        runOnUiThread {
            setWhoToCallLayoutGone()
            setCallLayoutVisible()
            binding.apply {
                rtcClient?.initializeSurfaceView(localView)
                rtcClient?.initializeSurfaceView(remoteView)
                rtcClient?.startLocalVideo(localView)
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

    private fun setIncomingCallLayoutGone() {
        binding.incomingCallLayout.gone()
    }

    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.show()
    }

    private fun setCallLayoutGone() {
        binding.callLayout.gone()
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


    private val peerConnectionObserverImpl = object : PeerConnectionObserver() {
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
            p0?.videoTracks?.get(0)?.addSink(binding.remoteView)
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
