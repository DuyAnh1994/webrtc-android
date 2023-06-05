package com.anhnd.webrtc.trios

import android.app.Application
import android.util.Log
import com.anhnd.webrtc.trios.callback.DataChannelObserverImpl
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
import org.webrtc.SdpObserver
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

    private val mediaConstraints = MediaConstraints().apply {
        mandatory?.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        optional?.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        optional.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        mandatory.add(MediaConstraints.KeyValuePair("levelControl", "true"))
        optional.add(MediaConstraints.KeyValuePair("levelControl", "true"))

        optional?.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        mandatory?.add(MediaConstraints.KeyValuePair("IceRestart", "true"))

        mandatory.add(MediaConstraints.KeyValuePair("maxHeight", Integer.toString(1920)));
        mandatory.add(MediaConstraints.KeyValuePair("maxWidth", Integer.toString(1080)));
        mandatory.add(MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(60)));
        mandatory.add(MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(30)))
    }

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

    fun createOffer() {
//        val mediaConstraints = MediaConstraints().apply {
//            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
//        }

        val sdpObserverByCreate = object : SdpObserverImpl() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, "1. createOffer ===> [SUCCESS]   type=[${desc?.type?.name}]   sdp=[ ... ]")
                setLocalOffer(desc)
            }
        }

        peerConnection?.createOffer(sdpObserverByCreate, mediaConstraints)
    }

    private fun setLocalOffer(sdp: SessionDescription?) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                Log.d(TAG, "2. setLocal(offer) ===> [SUCCESS]")
                sdp?.let {
                    localSdp = it.description
                    listener?.onSetLocalSdpOffer(state = State.SUCCESS, sdp = it)
                }
            }
        }

        peerConnection?.setLocalDescription(sdpObserver, sdp)
    }

    fun setRemoteAnswer(sdp: SessionDescription) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                Log.d(TAG, "3. setRemote(    ) ===> [SUCCESS] type[${sdp.type.name}]   sdp=[ ... ]")
            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "onSetFailure: $p0")
            }
        }

        peerConnection?.setRemoteDescription(sdpObserver, sdp)
    }

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)
    }

    fun createAnswer(target: String? = null, onSuccess: () -> Unit = {}) {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        }

        val sdpObserver = object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, "onCreateSuccess() called with: desc = $desc")
                setLocalDesc(desc)
                onSuccess.invoke()
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

        peerConnection?.createAnswer(sdpObserver, mediaConstraints)
    }

    private fun setLocalDesc(desc: SessionDescription?) {
        val sdpObserver = object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }

        peerConnection?.setLocalDescription(sdpObserver, desc)
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

            localVideoTrack = peerFactory.createVideoTrack("local_track", localVideoSource)
            localVideoTrack?.addSink(surface)
            localAudioTrack = peerFactory.createAudioTrack("local_track_audio", localAudioSource)

            val localStream = peerFactory.createLocalMediaStream("local_stream")
            localStream.addTrack(localAudioTrack)
            localStream.addTrack(localVideoTrack)
            peerConnection?.addTrack(localAudioTrack, listOf(localStream.id))
            peerConnection?.addTrack(localVideoTrack, listOf(localStream.id))

//            peerConnection?.addStream(localStream)
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

            // tạm chưa sử dụng
//            iceTransportsType = PeerConnection.IceTransportsType.ALL
//            bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT
//            disableIpv6 = true
//            disableIPv6OnWifi = true
//            iceBackupCandidatePairPingInterval = 1000
//            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
        }

        pc?.setConfiguration(rtcConfiguration)

        return pc
    }
}
