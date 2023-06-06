package com.anhnd.webrtc.trios

import android.app.Application
import android.util.Log
import com.anhnd.webrtc.trios.model.call.request.DataDtoRequest
import com.anhnd.webrtc.trios.model.call.request.RtcDtoRequest
import com.anhnd.webrtc.utils.TAG
import org.webrtc.AudioTrack
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
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class TriosRTCClient(
    private val application: Application,
    private val socket: TriosSocket,
    private val observer: PeerConnection.Observer
) {

    companion object {
        //        private const val RTC_URL = "turn:turn-dev01.gtrios.io:3478"
        private const val RTC_URL = "turn:dev.turn2.gtrios.io:3478"
        private const val USERNAME = "bgldemo"
        private const val PASSWORD = "bgltest"
    }

    /**
     * peer
     */
    private val peerFactory by lazy { createPeerConnectionFactory() }
    private val peerConnection by lazy { createPeerConnection() }
    private val eglContext = EglBase.create()
    private val iceServer = listOf(
        PeerConnection.IceServer.builder(RTC_URL)
            .setUsername(USERNAME)
            .setPassword(PASSWORD)
            .createIceServer()
    )


    /**
     * local
     */
    private val localVideoSource by lazy { peerFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerFactory.createAudioSource(MediaConstraints()) }
    private var localVideoCapturer: CameraVideoCapturer? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    var localSdp: String? = ""


    private val constraints = MediaConstraints().apply {
        mandatory?.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        optional?.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))

        mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
        optional.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))

        mandatory.add(MediaConstraints.KeyValuePair("levelControl", "true"))
        optional.add(MediaConstraints.KeyValuePair("levelControl", "true"))

        optional?.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        mandatory?.add(MediaConstraints.KeyValuePair("IceRestart", "true"))

        mandatory.add(MediaConstraints.KeyValuePair("maxHeight", Integer.toString(1920)));
        mandatory.add(MediaConstraints.KeyValuePair("maxWidth", Integer.toString(1080)));
        mandatory.add(MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(60)));
        mandatory.add(MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(1)))
    }


    init {
        initPeerConnectionFactory(application)
    }








    fun initializeSurfaceView(surface: SurfaceViewRenderer) {
        surface.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext, null)
        }
    }

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        try {
            val surfaceTextureHelper =
                SurfaceTextureHelper.create(Thread.currentThread().name, eglContext.eglBaseContext)
            localVideoCapturer = getVideoCapturer(application)
            localVideoCapturer?.initialize(
                surfaceTextureHelper,
                surface.context,
                localVideoSource.capturerObserver
            )
            localVideoCapturer?.startCapture(1920, 1080, 60)

            localVideoTrack = peerFactory.createVideoTrack("local_video_track", localVideoSource)
            localVideoTrack?.addSink(surface)
            localAudioTrack = peerFactory.createAudioTrack("local_audio_track", localAudioSource)

            val localStream = peerFactory.createLocalMediaStream("local_stream")
            localStream.addTrack(localAudioTrack)
            localStream.addTrack(localVideoTrack)
            peerConnection?.addTrack(localAudioTrack, listOf(localStream.id))
            peerConnection?.addTrack(localVideoTrack, listOf(localStream.id))

//            peerConnection?.addStream(localStream) // TODO: crash nếu create instance peer connection with RTCConfiguration
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVideoCapturer(application: Application): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find { isFrontFacing(it) }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }

    fun createOffer(target: String? = null) {

        val sdpObserverByCreate = object : SdpObserverImpl() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                setLocalDescAfterCreateOffer(target, desc)
            }
        }

        peerConnection?.createOffer(sdpObserverByCreate, constraints)
    }

    private fun setLocalDescAfterCreateOffer(
        target: String?,
        spAfterCreateOffer: SessionDescription?
    ) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                val dataDto = DataDtoRequest(
                    name = target,
                    sdp = spAfterCreateOffer?.description
                )

                val rtcDto = RtcDtoRequest(
                    type = "cmd",
                    transId = 0,
                    name = "join",
                    dataDto = dataDto
                )

                // send socket cmd create offer
                socket.sendMessageToSocket(rtcDto)
            }
        }

        peerConnection?.setLocalDescription(sdpObserver, spAfterCreateOffer)
    }

    fun setRemoteDesc(session: SessionDescription) {
        Log.d(TAG, "setRemoteDesc: 1")
        val sdpObserver = object : SdpObserverImpl() {}

        peerConnection?.setRemoteDescription(sdpObserver, session)
    }

    fun createAnswer(target: String? = null, onSuccess: (desc: SessionDescription?) -> Unit = {}) {
        val sdpObserver = object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, "onCreateSuccess() called with: desc = $desc")
                setLocalDesc(desc)
                onSuccess.invoke(desc)
            }

            override fun onSetSuccess() {
                Log.d(TAG, "onSetSuccess() called")
            }

            override fun onCreateFailure(p0: String?) {
                Log.d(TAG, "onCreateFailure() called with: p0 = $p0")
            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "onSetFailure() called with: p0 = $p0")
            }
        }

        Log.d(TAG, "answer signalingState: ${peerConnection?.signalingState()}")

        peerConnection?.createAnswer(sdpObserver, constraints)
    }

    private fun setLocalDesc(desc: SessionDescription?) {
        val sdpObserver = object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
//                val answer = hashMapOf("sdp" to desc?.description, "type" to desc?.type)

                // send socket cmd create answer
//                        socket.sendMessageToSocket("")

                Log.d(TAG, "onSetSuccess() called ccccccccccccccccccccccccccc")
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }

        peerConnection?.setLocalDescription(sdpObserver, desc)
    }

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)
    }


    /**
     * peer connection
     */
    private fun initPeerConnectionFactory(application: Application) {
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val encoderFactory = DefaultVideoEncoderFactory(eglContext.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglContext.eglBaseContext)
        val option = PeerConnectionFactory.Options()

        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(option)

        return builder.createPeerConnectionFactory()
    }

//    private fun createPeerConnection(): PeerConnection? {
//        val rtcConfiguration = RTCConfiguration(iceServer).apply {
//            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
//            iceTransportsType = PeerConnection.IceTransportsType.ALL
//            bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT
//            disableIpv6 = true
//            disableIPv6OnWifi = true
//            iceBackupCandidatePairPingInterval = 1000
//            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
//        }
//
//        return peerFactory.createPeerConnection(rtcConfiguration, observer)
//    }

    private fun createPeerConnection(): PeerConnection? {
        val rtcConfiguration = RTCConfiguration(iceServer).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT
            disableIpv6 = true
            disableIPv6OnWifi = true
            iceBackupCandidatePairPingInterval = 1000
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
        }

        val pc = peerFactory.createPeerConnection(rtcConfiguration, observer)

        return pc
    }
}
