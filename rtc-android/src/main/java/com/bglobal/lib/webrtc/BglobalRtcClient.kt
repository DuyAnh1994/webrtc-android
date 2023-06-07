package com.bglobal.lib.webrtc

import android.app.Application
import android.os.Build
import android.util.Log
import com.bglobal.lib.webrtc.callback.SdpObserverImpl
import com.bglobal.lib.utils.TAG
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.audio.JavaAudioDeviceModule

class BglobalRtcClient(
    private val application: Application,
    private val observer: PeerConnection.Observer,
    private val callback: Callback? = null
) {

    companion object {
        private const val RTC_URL = "turn:dev.turn2.gtrios.io:3478"
        private const val USERNAME = "bgldemo"
        private const val PASSWORD = "bgltest"
    }


    /**
     * peer
     */
    private val peerFactory by lazy { createPeerConnectionFactory() }
    private val peerConnection by lazy { createPeerConnection() }
    val eglBase by lazy { EglBase.create() }
    private val iceServer: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder(RTC_URL)
            .setUsername(USERNAME)
            .setPassword(PASSWORD)
            .createIceServer()
    )

    /**
     * local
     */
    private val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("RtpDataChannels", "true"))
    }

    private val localVideoSource by lazy { peerFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerFactory.createAudioSource(constraints) }
    var localSdp: String? = ""


    init {
        initPeerConnectionFactory()
    }

    /**
     * offer ===================================================================================
     */
    fun createOffer() {
//        val mediaConstraints = MediaConstraints().apply {
//            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
//        }

        val sdpObserverByCreate = object : SdpObserverImpl() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, "=> [SUCCESS] 1. createOffer")
                setLocalSdpByOffer(desc)
            }

            override fun onCreateFailure(p0: String?) {
                Log.d(TAG, "=> [FAIL] 1. createOffer reason=[$p0]")
            }
        }

        peerConnection?.createOffer(sdpObserverByCreate, constraints)
    }

    private fun setLocalSdpByOffer(sdp: SessionDescription?) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                Log.d(TAG, "=> [SUCCESS] 2. setLocalSdp type=[${sdp?.type?.name}]")
                sdp?.let {
                    localSdp = it.description
                    callback?.onSetLocalSdpOffer(state = State.SUCCESS, sdp = it)
                }
            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "=> [FAILURE] 2. setRemoteSdp type=[${sdp?.type?.name}] reason=[$p0]")
            }
        }

        peerConnection?.setLocalDescription(sdpObserver, sdp)
    }

    fun setRemoteSdpByOffer(sdp: SessionDescription?) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                Log.d(TAG, "=> [SUCCESS] 4. setRemoteSdp type=[${sdp?.type?.name}]")
            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "=> [FAILURE] 4. setRemoteSdp type=[${sdp?.type?.name}] reason=[$p0]")
            }
        }

        peerConnection?.setRemoteDescription(sdpObserver, sdp)
    }

    /**
     * answer ===================================================================================
     */
    fun createAnswer(onSuccess: (desc: SessionDescription?) -> Unit = {}) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, "=> [SUCCESS] 5. createAnswer")
                setLocalSdpByAnswer(desc)
                onSuccess.invoke(desc)
            }

            override fun onCreateFailure(p0: String?) {
                Log.d(TAG, "=> [FAIL] 5. createAnswer reason=[$p0]")
            }
        }

//        val mediaConstraints = MediaConstraints().apply {
//            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
//        }

        peerConnection?.createAnswer(sdpObserver, constraints)
    }

    private fun setLocalSdpByAnswer(sdp: SessionDescription?) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                Log.d(TAG, "=> [SUCCESS] 6. setLocalSdp type=[${sdp?.type?.name}]")
            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "=> [FAILURE] 6. setLocalSdp type=[${sdp?.type?.name}] reason=[$p0]")
            }
        }

        peerConnection?.setLocalDescription(sdpObserver, sdp)
    }

    fun setRemoteSdpByAnswer(sdp: SessionDescription?) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                Log.d(TAG, "=> [SUCCESS] 3. setRemoteSdp type=[${sdp?.type?.name}]")
            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "=> [FAILURE] 3. setRemoteSdp type=[${sdp?.type?.name}] reason=[$p0]")
            }
        }

        peerConnection?.setRemoteDescription(sdpObserver, sdp)
    }

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)
    }

    /**
     * camera
     */

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        try {
            val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglBase.eglBaseContext)
            val localVideoCapture = getCameraVideoCapture()
            localVideoCapture.initialize(surfaceTextureHelper, surface.context, localVideoSource.capturerObserver)
            localVideoCapture.startCapture(1920, 1080, 60)

            val localAudioTrack = peerFactory.createAudioTrack("local_audio_track", localAudioSource)
            val localVideoTrack = peerFactory.createVideoTrack("local_video_track", localVideoSource)
            localVideoTrack?.addSink(surface)

//            peerConnection?.addTrack(localAudioTrack)
            peerConnection?.addTrack(localVideoTrack)


//            peerConnection?.addTransceiver(localAudioTrack)
//            peerConnection?.addTransceiver(localVideoTrack)


//            val localStream = peerFactory.createLocalMediaStream("local_stream")
//            localStream.addTrack(localAudioTrack)
//            localStream.addTrack(localVideoTrack)


//            peerConnection?.addStream(localStream)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCameraVideoCapture(): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find { isFrontFacing(it) }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }

    /**
     * peer connection
     */

    private fun initPeerConnectionFactory() {
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        val option = PeerConnectionFactory.Options()

        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(option)
            .setAudioDeviceModule(JavaAudioDeviceModule.builder(application)
                .setUseHardwareAcousticEchoCanceler(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                .setUseHardwareNoiseSuppressor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

//                .setAudioRecordErrorCallback()
//                .setAudioTrackErrorCallback()
//                .setAudioRecordStateCallback()
//                .setAudioTrackStateCallback()

                .createAudioDeviceModule().also {
                    it.setMicrophoneMute(false)
                    it.setSpeakerMute(false)
                })

        return builder.createPeerConnectionFactory()
    }

    private fun createPeerConnection(): PeerConnection? {

        val rtcConfiguration = RTCConfiguration(iceServer).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
//            iceTransportsType = PeerConnection.IceTransportsType.ALL
//            bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT
//            disableIpv6 = true
//            disableIPv6OnWifi = true
//            iceBackupCandidatePairPingInterval = 1000
//            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
        }


        return peerFactory.createPeerConnection(rtcConfiguration, observer)
    }

    interface Callback {
        fun onSetLocalSdpOffer(state: State, sdp: SessionDescription)
    }

    enum class State {
        SUCCESS, ERROR
    }
}
