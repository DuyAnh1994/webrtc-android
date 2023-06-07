package com.anhnd.webrtc.p2p

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.R
import com.anhnd.webrtc.databinding.P2pCallActivityBinding
import com.anhnd.webrtc.p2p.models.IceCandidateModel
import com.anhnd.webrtc.p2p.models.MessageModel
import com.anhnd.webrtc.utils.PeerConnectionObserverImpl
import com.anhnd.webrtc.utils.RTCAudioManager
import com.google.gson.Gson
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription

class P2PCallActivity : AppCompatActivity(), NewMessageInterface {

    companion object {
        private const val TAG = "P2PCallActivity"
    }
    private lateinit var binding: P2pCallActivityBinding
    private var userName: String? = null
    private var socketRepository: SocketRepository? = null
    private var rtcClient: RTCClient? = null
    private var target: String = ""
    private val gson = Gson()
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private var isSpeakerMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = P2pCallActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        userName = intent.getStringExtra("username")
        socketRepository = SocketRepository(this)
        userName?.let { socketRepository?.initSocket(it) }


        rtcClient = RTCClient(
            application = application,
            username = userName!!,
            socketRepository = socketRepository!!,
            observer = peerConnectionObserverImpl
        )
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)


        binding.apply {
            callBtn.setOnClickListener {
//                socketRepository?.sendMessageToSocket(MessageModel(
//                    "start_call", userName, targetUserNameEt.text.toString(), null
//                ))
//                target = targetUserNameEt.text.toString()


                //we are ready for call, we started a call
                runOnUiThread {
                    setWhoToCallLayoutGone()
                    setCallLayoutVisible()
                    binding.apply {
                        rtcClient?.initializeSurfaceView(svrMyCamera)
                        rtcClient?.initializeSurfaceView(svr1)
                        rtcClient?.startLocalVideo(svrMyCamera)
                        rtcClient?.call(targetUserNameEt.text.toString())
                    }
                }
            }

            switchCameraButton.setOnClickListener {
                rtcClient?.switchCamera()
            }

            micButton.setOnClickListener {
                if (isMute) {
                    isMute = false
                    micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
                } else {
                    isMute = true
                    micButton.setImageResource(R.drawable.ic_baseline_mic_24)
                }
                rtcClient?.toggleAudio(isMute)
            }

            videoButton.setOnClickListener {
                if (isCameraPause) {
                    isCameraPause = false
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)
                } else {
                    isCameraPause = true
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
                }
                rtcClient?.toggleCamera(isCameraPause)
            }

            audioOutputButton.setOnClickListener {
                if (isSpeakerMode) {
                    isSpeakerMode = false
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_hearing_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                } else {
                    isSpeakerMode = true
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

                }

            }
            endCallButton.setOnClickListener {
                setCallLayoutGone()
                setWhoToCallLayoutVisible()
                setIncomingCallLayoutGone()
                rtcClient?.endCall()
            }
        }

    }

    override fun onNewMessage(message: MessageModel) {
        Log.d(TAG, "onNewMessage: $message")
        when (message.type) {
            "call_response" -> {
                if (message.data == "user is not online") {
                    //user is not reachable
                    runOnUiThread {
                        Toast.makeText(this, "user is not reachable", Toast.LENGTH_LONG).show()
                    }
                } else {
                    //we are ready for call, we started a call
                    runOnUiThread {
                        setWhoToCallLayoutGone()
                        setCallLayoutVisible()
                        binding.apply {
                            rtcClient?.initializeSurfaceView(svrMyCamera)
                            rtcClient?.initializeSurfaceView(svr1)
                            rtcClient?.startLocalVideo(svrMyCamera)
                            rtcClient?.call(targetUserNameEt.text.toString())
                        }
                    }
                }
            }

            "offer_received" -> {
                runOnUiThread {
                    setIncomingCallLayoutVisible()
                    binding.incomingNameTV.text = "${message.name.toString()} is calling you"

                    binding.acceptButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                        setCallLayoutVisible()
                        setWhoToCallLayoutGone()

                        binding.apply {
                            rtcClient?.initializeSurfaceView(svrMyCamera)
                            rtcClient?.initializeSurfaceView(svr1)
                            rtcClient?.startLocalVideo(svrMyCamera)
                        }
                        val session = SessionDescription(SessionDescription.Type.OFFER, message.data.toString())
                        Log.d(TAG, "onNewMessage  offer_received ccc: ${message.data}")
                        rtcClient?.onRemoteSessionReceived(session)
                        rtcClient?.answer(message.name!!)
                        target = message.name!!
                        binding.remoteViewLoading.visibility = View.GONE

                    }
                    binding.rejectButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                    }
                }
            }

            "answer_received" -> {

                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSessionReceived(session)
                runOnUiThread {
                    binding.remoteViewLoading.visibility = View.GONE
                }
            }

            "ice_candidate" -> {
                try {
                    val receivingCandidate = gson.fromJson(gson.toJson(message.data),
                        IceCandidateModel::class.java)
                    rtcClient?.addIceCandidate(IceCandidate(receivingCandidate.sdpMid,
                        Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()), receivingCandidate.sdpCandidate))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setIncomingCallLayoutGone() {
        binding.incomingCallLayout.visibility = View.GONE
    }

    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun setCallLayoutGone() {
        binding.callLayout.visibility = View.GONE
    }

    private fun setCallLayoutVisible() {
        binding.callLayout.visibility = View.VISIBLE
    }

    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.visibility = View.GONE
    }

    private fun setWhoToCallLayoutVisible() {
        binding.whoToCallLayout.visibility = View.VISIBLE
    }

    private val peerConnectionObserverImpl = object : PeerConnectionObserverImpl() {
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
            Log.d(TAG, "onIceCandidate() called with: p0 = $p0")

            rtcClient?.addIceCandidate(p0)

            val candidate = hashMapOf(
                "sdpMid" to p0?.sdpMid,
                "sdpMLineIndex" to p0?.sdpMLineIndex,
                "sdpCandidate" to p0?.sdp
            )

            socketRepository?.sendMessageToSocket(
                MessageModel("ice_candidate", userName, target, candidate)
            )
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
            Log.d(TAG, "onIceCandidatesRemoved() called with: p0 = ${p0?.count()}")
        }

        override fun onAddStream(p0: MediaStream?) {
            Log.d(TAG, "onAddStream() called with: p0 = ${p0?.id}")
            p0?.videoTracks?.get(0)?.addSink(binding.svr1)
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
