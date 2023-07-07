package com.bglobal.lib.webrtc

import android.app.Application
import android.os.Build
import android.util.Log
import com.bglobal.lib.utils.TAG
import com.bglobal.lib.webrtc.callback.SdpObserverImpl
import org.webrtc.AudioSource
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
import org.webrtc.RendererCommon
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.UUID

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
    val eglBase = EglBase.create()  // TODO: anhnd cần kt lại khả năng bị leak
    private val iceServer: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder(RTC_URL)
            .setUsername(USERNAME)
            .setPassword(PASSWORD)
            .createIceServer()
    )

    private val peerFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private val localAudioSource: AudioSource
    private val localVideoSource: VideoSource

    private val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))

        mandatory.add(MediaConstraints.KeyValuePair("maxWidth", "1080"))
        mandatory.add(MediaConstraints.KeyValuePair("maxHeight", "1920"))

        mandatory.add(MediaConstraints.KeyValuePair("minWidth", "640"))
        mandatory.add(MediaConstraints.KeyValuePair("minHeight", "480"))

        mandatory.add(MediaConstraints.KeyValuePair("maxFrameRate", "60"))
        mandatory.add(MediaConstraints.KeyValuePair("minFrameRate", "30"))

        mandatory.add(MediaConstraints.KeyValuePair("maxBitRate", "8000"))
        mandatory.add(MediaConstraints.KeyValuePair("minBitRate", "1000"))
    }
    private var localVideoCapture: CameraVideoCapturer? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    init {
        initPeerConnectionFactory()
        peerFactory = createPeerConnectionFactory()
        peerConnection = createPeerConnection()
        localAudioSource = peerFactory.createAudioSource(constraints)
        localVideoSource = peerFactory.createVideoSource(false)
    }

    /**
     * offer ===================================================================================
     */
    fun createOffer() {
        val sdpObserverByCreate = object : SdpObserverImpl() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, "=> [SUCCESS] 1. createOffer")
                setLocalSdpByOffer(desc)
            }

            override fun onCreateFailure(p0: String?) {
                Log.d(TAG, "=> [FAIL] 1. createOffer reason=[$p0]")
            }
        }

//        val constraints = MediaConstraints().apply {
//            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
//        }

        peerConnection?.createOffer(sdpObserverByCreate, constraints)
    }

    private fun setLocalSdpByOffer(sdp: SessionDescription?) {
        val sdpObserver = object : SdpObserverImpl() {
            override fun onSetSuccess() {
                Log.d(TAG, "=> [SUCCESS] 2. setLocalSdp type=[${sdp?.type?.name}]")
                sdp?.let {
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

//        val constraints = MediaConstraints().apply {
//            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
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


    fun initLocalVideo(surface: SurfaceViewRenderer) {
        try {
            val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglBase.eglBaseContext)
            localVideoCapture = getCameraVideoCapture()
            localVideoCapture?.initialize(surfaceTextureHelper, surface.context, localVideoSource.capturerObserver)
            surface.setEnableHardwareScaler(true)
            surface.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

            localAudioTrack = peerFactory.createAudioTrack("local_audio_track", localAudioSource)
            localVideoTrack = peerFactory.createVideoTrack("local_video_track", localVideoSource)

            val id = UUID.randomUUID().toString()
            peerConnection?.addTrack(localAudioTrack, listOf(id))
            peerConnection?.addTrack(localVideoTrack, listOf(id))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun turnOnCamera() {
        localVideoCapture?.startCapture(1920, 1080, 60)
    }

    fun turnOffCamera() {
        localVideoCapture?.stopCapture()
    }

    fun addSinkLocal(surface: SurfaceViewRenderer) {
        try {
            localVideoTrack?.addSink(surface)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeSinkLocal(surface: SurfaceViewRenderer) {
        try {
            localVideoTrack?.removeSink(surface)
            surface.release()
//            localVideoTrack?.dispose()
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

    fun switchCamera(handler: CameraVideoCapturer.CameraSwitchHandler? = null) {
        localVideoCapture?.switchCamera(handler)
    }

    fun toggleAudio(enable: Boolean) {
        localAudioTrack?.setEnabled(enable)
    }

    fun volume(volume: Double) {
        localAudioTrack?.setVolume(volume)
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
            bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        }

        return peerFactory.createPeerConnection(rtcConfiguration, observer)
    }

    fun close() {
        try {
            localAudioTrack?.dispose()
            localVideoTrack?.dispose()
            peerConnection?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface Callback {
        fun onSetLocalSdpOffer(state: State, sdp: SessionDescription)
    }

    enum class State {
        SUCCESS, ERROR
    }
}
