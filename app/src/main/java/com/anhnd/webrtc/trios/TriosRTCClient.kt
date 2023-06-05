package com.anhnd.webrtc.trios

import android.app.Application
import android.util.Log
import com.anhnd.webrtc.trios.callback.RTCListener
import com.anhnd.webrtc.trios.callback.SdpObserverImpl
import com.anhnd.webrtc.trios.callback.State
import com.anhnd.webrtc.utils.TAG
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
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
import org.webrtc.VideoTrack

class TriosRTCClient(
    private val application: Application,
    private val observer: PeerConnection.Observer,
    private val listener: RTCListener? = null
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
    private val iceServer: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder(RTC_URL)
            .setUsername(USERNAME)
            .setPassword(PASSWORD)
            .createIceServer()
    )
    private var dataChannel: DataChannel? = null

    /**
     * local
     */
    private val localVideoSource by lazy { peerFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerFactory.createAudioSource(MediaConstraints()) }
    private var localVideoCapturer: CameraVideoCapturer? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    var localSdp: String? = ""


    init {
        initPeerConnectionFactory()
    }

    /**
     * offer ===================================================================================
     */
    fun createOffer() {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        }

        val sdpObserverByCreate = object : SdpObserverImpl() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, "=> [SUCCESS] 1. createOffer")
                setLocalSdpByOffer(desc)
            }

            override fun onCreateFailure(p0: String?) {
                Log.d(TAG, "=> [FAIL] 1. createOffer reason=[$p0]")
            }
        }

        peerConnection?.createOffer(sdpObserverByCreate, mediaConstraints)
    }

    private fun setLocalSdpByOffer(sdp: SessionDescription?) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                Log.d(TAG, "=> [SUCCESS] 2. setLocalSdp type=[${sdp?.type?.name}]")
                sdp?.let {
                    localSdp = it.description
                    listener?.onSetLocalSdpOffer(state = State.SUCCESS, sdp = it)
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

        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(sdpObserver, mediaConstraints)
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

    fun initializeSurfaceView(surface: SurfaceViewRenderer) {
        surface.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext, null)
        }
    }

    private fun getCameraVideoCapturer(): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find { isFrontFacing(it) }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        try {
            val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglContext.eglBaseContext)
            localVideoCapturer = getCameraVideoCapturer()
            localVideoCapturer?.initialize(surfaceTextureHelper, surface.context, localVideoSource.capturerObserver)
            localVideoCapturer?.startCapture(1920, 1080, 60)

            localVideoTrack = peerFactory.createVideoTrack("local_video_track", localVideoSource)
            localVideoTrack?.addSink(surface)
            localAudioTrack = peerFactory.createAudioTrack("local_audio_track", localAudioSource)

            val localStream = peerFactory.createLocalMediaStream("local_stream")
            localStream.addTrack(localAudioTrack)
            localStream.addTrack(localVideoTrack)

            // k sử dụng vẫn có thể stream
//            peerConnection?.addTrack(localAudioTrack, listOf(localStream.id))
//            peerConnection?.addTrack(localVideoTrack, listOf(localStream.id))

            peerConnection?.addStream(localStream)
        } catch (e: Exception) {
            e.printStackTrace()
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
        val encoderFactory = DefaultVideoEncoderFactory(
            eglContext.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglContext.eglBaseContext)
        val option = PeerConnectionFactory.Options()

        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(option)

        return builder.createPeerConnectionFactory()
    }

    private fun createPeerConnection(): PeerConnection? {
        val pc = peerFactory.createPeerConnection(iceServer, observer)

        val rtcConfiguration = RTCConfiguration(iceServer).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT
            disableIpv6 = true
            disableIPv6OnWifi = true
            iceBackupCandidatePairPingInterval = 1000
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
        }

        pc?.setConfiguration(rtcConfiguration)

        return pc
    }
}
