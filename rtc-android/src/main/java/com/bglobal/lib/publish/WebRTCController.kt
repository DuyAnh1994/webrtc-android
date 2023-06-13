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
import com.bglobal.lib.webrtc.data.model.call.peer.PeerDTO
import com.bglobal.lib.webrtc.data.model.call.toParticipantRTC
import com.bglobal.lib.webrtc.data.socket.BglobalSocketClient
import com.bglobal.lib.webrtc.data.socket.BglobalSocketListener
import com.bglobal.lib.webrtc.data.socket.HandleModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.IceCandidate
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
    private var username = ""
    private val participantRTCList = mutableListOf<ParticipantRTC>()

    init {

    }

    fun build() {
        socket = BglobalSocketClient(
            commandListener = commandListener,
            responseListener = responseListener,
            eventListener = eventListener,
            errorListener = errorListener
        )
        rtcClient = BglobalRtcClient(
            application = this.application,
            observer = peerConnectionObserverImpl,
            callback = callback
        )
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        toggleAudio(true)
    }

    fun startCall(name: String) {
        username = name
        rtcClient?.createOffer()
    }

    private fun onCreateOffer(sdp: String?) {
        if (sdp == null) {
            throw RtcException("sdp must not null")
        }
        val request = handleModel.createOffer(name = username, sdp = sdp)
        socket?.sendMessageToSocket(request)
    }

    private fun offerResponse(sdp: String?) {
        /*
         * set remote với type == ANSWER để stream lên server
         */
        val session = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        rtcClient?.setRemoteSdpByAnswer(session)
    }

    private fun createAnswer(sdp: String?) {
        val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
        rtcClient?.setRemoteSdpByOffer(offer)
        rtcClient?.createAnswer {
            CoroutineScope(Dispatchers.IO).launch {
                val request = handleModel.updateSdp(it?.description)
                socket?.sendMessageToSocket(request)
            }
        }
    }

    fun getEglBase(): EglBase {
        return rtcClient?.eglBase ?: throw RtcException("EglBase instance not yet initialization")
    }

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        rtcClient?.startLocalVideo(surface)
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

    fun toggleAudio(mute: Boolean) {
        rtcClient?.toggleAudio(mute)
    }

    private fun getRoomState(newList: List<ParticipantRTC>): RoomState {
        return when {
            newList.count() == participantRTCList.count() -> RoomState.NONE
            newList.count() > participantRTCList.count() -> RoomState.USER_JOIN
            newList.count() < participantRTCList.count() -> RoomState.USER_LEAVE
            else -> RoomState.NONE
        }
    }

//    private fun getRoomState(newList: List<String>): RoomState {
//        return when {
//            newList.count() == participantRTCList.count() -> RoomState.NONE
//            newList.count() > participantRTCList.count() -> RoomState.USER_JOIN
//            newList.count() < participantRTCList.count() -> RoomState.USER_LEAVE
//            else -> RoomState.NONE
//        }
//    }

    private fun findUserInRoom(newList: List<ParticipantRTC>): ParticipantRTC? {
        var parti: ParticipantRTC? = null
        newList.forEach { newItem ->
            if (participantRTCList.isEmpty()) {
                return newItem
            }
            participantRTCList.forEach { oldItem ->
                Log.d(TAG, "findUserInRoom: ${newItem.name}    =========    ${oldItem.name}")
                if (newItem.name != oldItem.name) {
                    parti = newItem
                }
            }
        }
        return parti
    }

    private fun mapConvertToList(map: HashMap<String, String>?): List<ParticipantDTO> {
        val list = mutableListOf<ParticipantDTO>()

        map?.forEach { (k, v) ->
            val item = list.firstOrNull { it.name == v }
            if (item == null) {
                val parti = ParticipantDTO(
                    name = v,
                    streamId = k
                )
                list.add(parti)
            }
        }

        return list
    }

//    private fun findUserLeaveRoom(newList: List<ParticipantRTC>): ParticipantRTC? {
//        var parti: ParticipantRTC? = null
//        participantRTCList.forEach { oldItem ->
//            newList.forEach { newItem ->
//                if (newItem.id != oldItem.id) {
//                    parti = newItem
//                }
//            }
//        }
//        return parti
//    }

    private val callback = object : BglobalRtcClient.Callback {
        override fun onSetLocalSdpOffer(state: BglobalRtcClient.State, sdp: SessionDescription) {
            onCreateOffer(sdp = sdp.description)
        }
    }

    private val peerConnectionObserverImpl = object : PeerConnectionObserverImpl() {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            rtcClient?.addIceCandidate(iceCandidate)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            rtcListener?.onAddStream(mediaStream)
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
            rtcListener?.onRemoveStream(mediaStream)
        }
    }

    private val commandListener = object : BglobalSocketListener.Command {
        override fun onAnswer(rtcDto: AnswerResponse) {
            createAnswer(rtcDto.getSdp())
        }
    }

    private val responseListener = object : BglobalSocketListener.Response {
        override fun onOffer(response: OfferResponse) {
            offerResponse(response.getSdp())
            CoroutineScope(Dispatchers.IO).launch {
                val request = handleModel.getPeer(username)
                socket?.sendMessageToSocket(request)

//                val partiList = response.dataDto?.roomDto?.participants?.map {
//                    it.toParticipantRTC()
//                } ?: mutableListOf()
//
//                val user = ParticipantRTC(
//                    id = 0,
//                    name = "name",
//                    streamId = "mediaStreamID"
//                )
//
//                rtcListener?.onUserJoinRoom(user)
            }
            createAnswer(response.getSdp())
        }

        override fun onPeer(peerDTO: PeerDTO) {
//            val participantDTO = handleModel.getParticipant(response.data)
//            participantDTO?.toParticipantRTC()?.let { rtcListener?.onUserJoinRoom(it) }

//            if (participantRTCList.isEmpty()) {
//                peerDTO.map?.forEach { (k, v) ->
//                    val item = ParticipantRTC(
//                        id = -1,
//                        name = v,
//                        streamId = k
//                    )
//                    participantRTCList.add(item)
//                }
//            } else {
//                peerDTO.map?.forEach { (k, v) ->
//                    participantRTCList.forEach {
//                        if (it.name == v) {
//                            it.streamId = k
//                        }
//                    }
//                }
//            }


//            val keyList = peerDTO.map?.keys?.toList() ?: emptyList()
            val list = mapConvertToList(peerDTO.map).map { it.toParticipantRTC() }
            val state = getRoomState(list)
            Log.d(TAG, "onPeer state: $state")
            when (state) {
                RoomState.USER_JOIN -> {
                    val user = findUserInRoom(list)
                    user?.let { rtcListener?.onUserJoinRoom(it) }
                }

                RoomState.USER_LEAVE -> {
                    val user = findUserInRoom(list)
                    user?.let { rtcListener?.onUserLeaveRoom(it) }
                }

                else -> {}
            }

            participantRTCList.replace(list)
            rtcListener?.onUserListInRoom(participantRTCList)
        }
    }

    private val eventListener = object : BglobalSocketListener.Event {
        override fun onParticipantList(participantList: List<ParticipantDTO>) {
            CoroutineScope(Dispatchers.IO).launch {
//                val list = participantList.map { it.toParticipantRTC() }
//
//                when (getRoomState(list)) {
//                    RoomState.USER_JOIN -> {
//                        val newUser = findUserJoinRoom(list)
//                        newUser?.let {
//                            rtcListener?.onUserJoinRoom(it)
//                        }
//                    }
//
//                    RoomState.USER_LEAVE -> {
//                        val newUser = findUserJoinRoom(list)
//                        newUser?.let {
//                            rtcListener?.onUserLeaveRoom(it)
//                        }
//                    }
//
//                    else -> {}
//                }
//
//                participantRTCList.replace(list)
//                rtcListener?.onUserListInRoom(participantRTCList)

                /*
                 - tạm thời xử lý như sau:
                 nếu mình đang ở trong room thì callback này được gọi khi có user join room,
                 lúc này gọi peer để lấy thông tin các user trong room. bao gồm cả id, name, streamId
                 */
                val request = handleModel.getPeer(username)
                socket?.sendMessageToSocket(request)
            }
        }
    }

    private val errorListener = object : BglobalSocketListener.Error {
        override fun onError(reason: String) {
            Log.d(TAG, "onError() called with: reason = $reason")
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
