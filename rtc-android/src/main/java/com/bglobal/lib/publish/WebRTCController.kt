package com.bglobal.lib.publish

import android.app.Application
import android.util.Log
import com.bglobal.lib.utils.replace
import com.bglobal.lib.webrtc.BglobalRtcClient
import com.bglobal.lib.webrtc.RTCAudioManager
import com.bglobal.lib.webrtc.callback.PeerConnectionObserverImpl
import com.bglobal.lib.webrtc.data.RtcException
import com.bglobal.lib.webrtc.data.model.call.ParticipantDTO
import com.bglobal.lib.webrtc.data.model.call.answer.AnswerResponse
import com.bglobal.lib.webrtc.data.model.call.offer.OfferResponse
import com.bglobal.lib.webrtc.data.model.call.option.OptionDTO
import com.bglobal.lib.webrtc.data.model.call.toParticipantRTC
import com.bglobal.lib.webrtc.data.socket.BglobalSocketClient
import com.bglobal.lib.webrtc.data.socket.BglobalSocketListener
import com.bglobal.lib.webrtc.data.socket.HandleModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.java_websocket.handshake.ServerHandshake
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class WebRTCController(private val application: Application) {

    companion object {
        private const val TAG = "WebRTCController"
    }

    private var rtcClient: BglobalRtcClient? = null
    private var rtcListener: BglobalRtcListener? = null
    private var socket: BglobalSocketClient? = null
    private val handleModel by lazy { HandleModel() }
    private val rtcAudioManager by lazy { RTCAudioManager.create(application.applicationContext) }
    private var localName = ""
    private val participantRTCList = mutableListOf<ParticipantRTC>()
    private val streamWaitingHandleList = mutableListOf<StreamAsync>()
    private var addMediaStream: StreamAsync? = null
    private var transIdByPeer = 0
    private val mainThread = CoroutineScope(Dispatchers.Main)

    init {

    }

    fun build(roomId: String) {
        socket = BglobalSocketClient(
            commonListener = commonListener,
            commandListener = commandListener,
            responseListener = responseListener,
            eventListener = eventListener,
            errorListener = errorListener
        )
        socket?.roomId = roomId
        socket?.connect()
        rtcClient = BglobalRtcClient(
            application = this.application,
            observer = peerConnectionObserverImpl,
            callback = callback
        )
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
    }

    fun startCall(name: String) {
        localName = name
        rtcClient?.createOffer()
    }

    fun endCall() {
        rtcClient?.close()
        socket?.close()
    }

    private fun onCreateOffer(sdp: String?) {
        if (sdp == null) {
            throw RtcException("sdp must not null")
        }
        val request = handleModel.createOffer(name = localName, sdp = sdp)
        socket?.sendMessageToSocket(request, "create_offer")
    }

    private fun offerResponse(joinedSdp: String?, onSuccess: (() -> Unit) = {}) {
        /*
         * set remote với type == ANSWER để stream lên server
         */
        val session = SessionDescription(SessionDescription.Type.ANSWER, joinedSdp)
        rtcClient?.setRemoteSdpByAnswer(session)
        onSuccess.invoke()
    }

    fun updateOffer(rtcDto: AnswerResponse) {
//        Log.d(TAG, "updateOffer  sdp  : $sdp")
        /*
         * chú ý: phải sử dụng sdp mà server trả về với type=cmd - name=update thì mới stream lên server được
         */
        val offer = SessionDescription(SessionDescription.Type.OFFER, rtcDto.getSdp())
        rtcClient?.setRemoteSdpByOffer(offer)

        rtcClient?.createAnswer {
            CoroutineScope(Dispatchers.IO).launch {
                /*
                * chú ý: sdp truyền lên phải là sdp sau khi create answer tù instance của peer connection
                *
                * */
                val request = handleModel.updateSdp(transId = rtcDto.getTransId(), sdp = it?.description)
                socket?.sendMessageToSocket(request, "update")
            }
        }
    }

    fun getEglBase(): EglBase {
        return rtcClient?.eglBase ?: throw RtcException("EglBase instance not yet initialization")
    }

    fun initLocalVideo(surface: SurfaceViewRenderer) {
        rtcClient?.initLocalVideo(surface)
    }

    fun turnOnCamera() {
        rtcClient?.turnOnCamera()
    }

    fun turnOffCamera() {
        rtcClient?.turnOffCamera()
    }

    fun addSinkLocal(surface: SurfaceViewRenderer) {
        rtcClient?.addSinkLocal(surface)
    }

    fun removeSinkLocal(surface: SurfaceViewRenderer) {
        rtcClient?.removeSinkLocal(surface)
    }

    fun addRtcListener(listener: BglobalRtcListener) {
        try {
            rtcListener = listener
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeRctListener() {
        rtcListener = null
    }

    fun switchCamera() {
        rtcClient?.switchCamera(cameraSwitchHandler)
    }

    fun peer() {
        val request = handleModel.getPeer(transId = transIdByPeer, name = localName)
        socket?.sendMessageToSocket(request, "peer")
        ++transIdByPeer
    }

    fun changeCamera(isTurnOffCamera: Boolean) {
        val json = handleModel.getJsonChangeOption(isTurnOffCamera = isTurnOffCamera)
        dataChannel(localName, json)
    }

    fun toggleAudio(mute: Boolean) {
        val json = handleModel.getJsonChangeOption(mute = mute)
        dataChannel(localName, json)
        rtcClient?.toggleAudio(!mute)
    }

    private fun dataChannel(name: String, command: String) {
        val request = handleModel.getControllerRequest(name = name, command = command)
        socket?.sendMessageToSocket(request, "ctrl")
    }

    private val callback = object : BglobalRtcClient.Callback {
        override fun onSetLocalSdpOffer(state: BglobalRtcClient.State, sdp: SessionDescription) {
            onCreateOffer(sdp = sdp.description)
        }
    }

    private val peerConnectionObserverImpl = object : PeerConnectionObserverImpl() {
        override fun onAddStream(track: MediaStream?) {
            Log.d(TAG, "\n\nonAddStream: ${track?.id}")

            peer()

            /*
             * - gán transId tương ứng mỗi lần gọi peer, để sau khi nhận được response của peer thì
             *  sẽ xử lý với transId tương ứng
             * - mỗi lần xử lý xong thì phải clear item trong stream waiting list
             */
            val stream = StreamAsync(
                id = track?.id,
                track = track,
                type = RoomState.USER_JOIN,
                transId = transIdByPeer
            )

            streamWaitingHandleList.add(stream)
        }

        override fun onRemoveStream(track: MediaStream?) {
            Log.d(TAG, "\n\nonRemoveStream: ${track?.id}")

            peer()
        }
    }

    private val commandListener = object : BglobalSocketListener.Command {
        override fun onUpdateOffer(rtcDto: AnswerResponse) {
            updateOffer(rtcDto)
        }
    }

    private val responseListener = object : BglobalSocketListener.Response {
        override fun onOffer(response: OfferResponse) {
            offerResponse(response.getSdp()) {}
        }

        override fun onPeer(participantDTOList: List<ParticipantDTO>) {
            val dtoList = participantDTOList.map {
                it.toParticipantRTC()
            }

            val list = dtoList.toMutableList()

            Log.d(TAG, "\n\n onPeer 1   total = ${list.count()} ----------------------------------------------")
            list.forEach {
                Log.d(TAG, "onPeer 1: id=[${it.id}] name=[${it.name}] streamId=[${it.streamId} ms=${it.mediaStream}]")
            }

            // TODO: xử lý lại độ phức tạp sau

            list.forEachIndexed { _, v ->
                val streamWaiting = streamWaitingHandleList.find {
                    it.id == v.streamId
                }
                if (streamWaiting != null) {
                    v.mediaStream = streamWaiting.track
                    streamWaitingHandleList.remove(streamWaiting)
                    return@forEachIndexed
                }
            }

            list.forEachIndexed { i, v ->
                participantRTCList.forEach { oldItem ->
                    if (v.streamId == oldItem.streamId) {
                        val tempItem = v.copy()
                        tempItem.mediaStream = oldItem.mediaStream
                        list[i] = tempItem
                        return@forEach
                    }
                }
            }

            Log.d(TAG, "\n\n onPeer 2   total = ${list.count()} ----------------------------------------------")
            list.forEach {
                Log.d(TAG, "onPeer 2: id=[${it.id}] name=[${it.name}] streamId=[${it.streamId} ms=${it.mediaStream}]")
            }

            participantRTCList.replace(list)
            rtcListener?.onUserListInRoom(participantRTCList)
        }
    }

    private val eventListener = object : BglobalSocketListener.Event {
        override fun onOption(name: String?, option: OptionDTO) {
            val list = participantRTCList.toMutableList()

            var currItem: ParticipantRTC? = null
            list.forEach {
                if (name != localName && name == it.name) {
                    currItem = it
                    return@forEach
                }
            }

            currItem?.let {
                it.isTurnOffCamera = option.isTurnOffCamera
                it.isMute = option.mute

                rtcListener?.onOption(it)
            }
        }
    }

    private val errorListener = object : BglobalSocketListener.Error {
        override fun onError(reason: String) {
            Log.d(TAG, "onError() called with: reason = $reason")
        }
    }

    private val commonListener = object : BglobalSocketListener.Common {
        override fun onOpen(url: String, handshakedata: ServerHandshake?) {
            mainThread.launch {
                rtcListener?.onConnect(
                    url = url,
                    code = handshakedata?.httpStatus?.toInt() ?: 0,
                    msg = handshakedata?.httpStatusMessage.toString()
                )
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            mainThread.launch {
                rtcListener?.onDisconnect(code = code, reason = reason, remote = remote)
            }
        }

        override fun onError(ex: Exception?) {
            mainThread.launch {
                rtcListener?.onError(ex)
            }
        }
    }

    private val cameraSwitchHandler = object : CameraVideoCapturer.CameraSwitchHandler {
        override fun onCameraSwitchDone(p0: Boolean) {
            rtcListener?.onCameraSwitchDone(p0)
        }

        override fun onCameraSwitchError(p0: String?) {
            rtcListener?.onCameraSwitchError(p0.toString())
        }
    }
}
