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
import com.bglobal.lib.webrtc.data.model.call.peer.PeerResponse
import com.bglobal.lib.webrtc.data.model.call.toParticipantRTC
import com.bglobal.lib.webrtc.data.socket.BglobalSocketClient
import com.bglobal.lib.webrtc.data.socket.BglobalSocketListener
import com.bglobal.lib.webrtc.data.socket.HandleModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class RtcController(private val application: Application) {

    companion object {
        private const val TAG = "RtcManager"
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

    private fun getRoomState(newList: List<ParticipantRTC>): RoomState {
        return when {
            newList.count() == participantRTCList.count() -> RoomState.NONE
            newList.count() > participantRTCList.count() -> RoomState.USER_JOIN
            newList.count() < participantRTCList.count() -> RoomState.USER_LEAVE
            else -> RoomState.NONE
        }
    }

    private fun findUserJoinRoom(newList: List<ParticipantRTC>): ParticipantRTC? {
        var parti: ParticipantRTC? = null
        newList.forEach { newItem ->
            participantRTCList.forEach { oldItem ->
                if (newItem.id != oldItem.id) {
                    parti = newItem
                }
            }
        }
        return parti
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

        override fun onPeer(response: PeerResponse) {
//            val participantDTO = handleModel.getParticipant(response.data)
//
//            participantDTO?.toParticipantRTC()?.let { rtcListener?.onUserJoinRoom(it) }
        }
    }

    private val eventListener = object : BglobalSocketListener.Event {
        override fun onParticipantList(participantList: List<ParticipantDTO>) {
            CoroutineScope(Dispatchers.IO).launch {
                val list = participantList.map { it.toParticipantRTC() }

                when (getRoomState(list)) {
                    RoomState.USER_JOIN -> {
                        val newUser = findUserJoinRoom(list)
                        newUser?.let {
                            rtcListener?.onUserJoinRoom(it)
                        }
                    }

                    RoomState.USER_LEAVE -> {
                        val newUser = findUserJoinRoom(list)
                        newUser?.let {
                            rtcListener?.onUserLeaveRoom(it)
                        }
                    }

                    else -> {}
                }

                participantRTCList.replace(list)
                rtcListener?.onUserListInRoom(participantRTCList)

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
}
