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
import org.webrtc.MediaStreamTrack
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
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
    private var myUser: ParticipantRTC? = null
    private var username = ""
    private val participantRTCList = mutableListOf<ParticipantRTC>()

    //    private val mediaStreamList = mutableListOf<MediaStream>()
    private var isOnAddTrackRunning = false

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
        myUser = ParticipantRTC(
            id = 0,
            name = username,
            streamIdOrigin = "",
            streamIdSecondary = mutableListOf()
        )
        myUser?.let {
            participantRTCList.add(0, it)
            rtcListener?.onUserListInRoom(participantRTCList)
        }
        rtcClient?.createOffer()
    }

    private fun onCreateOffer(sdp: String?) {
        if (sdp == null) {
            throw RtcException("sdp must not null")
        }
        val request = handleModel.createOffer(name = username, sdp = sdp)
        socket?.sendMessageToSocket(request, "create_offer")
    }

    private fun offerResponse(joinedSdp: String?) {
        /*
         * set remote với type == ANSWER để stream lên server
         */
        val session = SessionDescription(SessionDescription.Type.ANSWER, joinedSdp)
        rtcClient?.setRemoteSdpByAnswer(session)
    }

    private fun updateOffer(sdp: String?) {
//        Log.d(TAG, "updateOffer  sdp  : $sdp")
        /*
         * chú ý: phải sử dụng sdp mà server trả về với type=cmd - name=update thì mới stream lên server được
         */
        val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
        rtcClient?.setRemoteSdpByOffer(offer)
        rtcClient?.createAnswer {
            CoroutineScope(Dispatchers.IO).launch {
                /*
                * chú ý: sdp truyền lên phải là sdp sau khi create answer tù instance của peer connection
                *
                * */
                val request = handleModel.updateSdp(it?.description)
                socket?.sendMessageToSocket(request, "update")
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

    fun peer() {
        val request = handleModel.getPeer(username)
        socket?.sendMessageToSocket(request, "peer")
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
            } else {
                participantRTCList.forEach { oldItem ->
                    Log.d(TAG, "findUserInRoom: ${newItem.name}    =========    ${oldItem.name}")
                    if (newItem.name != oldItem.name) {
                        parti = newItem
                    }
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
                    streamIdOrigin = k
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

//    private val peerConnectionObserverImpl = object : PeerConnection.Observer {
//        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
//        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
//        override fun onIceConnectionReceivingChange(p0: Boolean) {}
//        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
//        override fun onIceCandidate(p0: IceCandidate?) {
//            rtcClient?.addIceCandidate(p0)
//        }
//
//        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
//        override fun onAddStream(p0: MediaStream?) {}
//        override fun onRemoveStream(p0: MediaStream?) {}
//        override fun onDataChannel(p0: DataChannel?) {}
//        override fun onRenegotiationNeeded() {}
//        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
//            Log.d(TAG, "\n\n onAddTrack =================================")
//            p1?.forEach {
//                Log.d(TAG, "id=[$it]")
//            }
//        }
//    }

    private val peerConnectionObserverImpl = object : PeerConnectionObserverImpl() {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            rtcClient?.addIceCandidate(iceCandidate)
        }

        override fun onTrack(transceiver: RtpTransceiver?) {
//            transceiver?.sender?.streams?.forEach {
//                Log.d(TAG, "onTrack ccccccc: $it")
//            }
            Log.d(TAG, "onTrack: ${transceiver?.sender?.track()}")
        }

        override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreamList: Array<out MediaStream>?) {

//            Log.d(TAG, "onAddTrack: rtpReceiver_id: ${rtpReceiver?.id()}")

            val track : MediaStreamTrack = rtpReceiver?.track() ?: return
            Log.d(TAG, "onAddTrack: id=${track.id()} kind=${track.id()}")



            Log.d(TAG, "\n\n onAddTrack =================================")
            mediaStreamList?.forEach {
                Log.d(TAG, "id=[$it]")
            }

//            isOnAddTrackRunning = true
//            peer()


//            participantRTCList.forEach { rtc ->
//                mediaStreamList?.forEach { ms ->
//                    Log.d(TAG, "onAddTrack: ${rtc.streamIdSecondary}    ----     ${ms.id}")
//                    if (rtc.streamIdSecondary.contains(ms.id)) {
//                        rtc.mediaStream = ms
//                    }
//                }
//            }
//
//
//            rtcListener?.onUserListInRoom(participantRTCList)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
//            Log.d(TAG, "\n\nonAddStream: ${mediaStream?.id}")
            rtcListener?.onAddStream(mediaStream)
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
            rtcListener?.onRemoveStream(mediaStream)
        }
    }

    private val commandListener = object : BglobalSocketListener.Command {
        override fun onUpdateOffer(rtcDto: AnswerResponse) {
            updateOffer(rtcDto.getSdp())
        }
    }

    private val responseListener = object : BglobalSocketListener.Response {
        override fun onOffer(response: OfferResponse) {
            offerResponse(response.getSdp())
            CoroutineScope(Dispatchers.IO).launch {
                val request = handleModel.getPeer(username)
                socket?.sendMessageToSocket(request, "peer")
            }
        }

        override fun onPeer(participantDTOList: List<ParticipantDTO>) {
            val dtoList = participantDTOList.map {
                it.toParticipantRTC()
            }

//            when (getRoomState(dtoList)) {
//                RoomState.USER_JOIN -> {
//                    val user = findUserInRoom(dtoList)
//                    user?.let {
//                        rtcListener?.onUserJoinRoom(it)
//                    }
//                }
//
//                RoomState.USER_LEAVE -> {
//                    val user = findUserInRoom(dtoList)
//                    user?.let {
//                        rtcListener?.onUserLeaveRoom(it)
//                    }
//                }
//
//                else -> {}
//            }
            Log.d(TAG, "\nonPeer: $dtoList")

            participantRTCList.replace(dtoList)
//            rtcListener?.onUserListInRoom(participantRTCList)
        }
    }

    private val eventListener = object : BglobalSocketListener.Event {
        override fun onParticipantList(participantList: List<ParticipantDTO>) {
            CoroutineScope(Dispatchers.IO).launch {
                /*
                 - tạm thời xử lý như sau:
                 nếu mình đang ở trong room thì callback này được gọi khi có user join room,
                 lúc này gọi peer để lấy thông tin các user trong room. bao gồm cả id, name, streamId
                 */

                // TODO: xử lý ở đây đang bất đồng bộ, xem xét gọi peer ở onAddStream/onAddTrack
                if (!isOnAddTrackRunning) {
                    val request = handleModel.getPeer(username)
                    socket?.sendMessageToSocket(request, "peer")
                }
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
