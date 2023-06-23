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
import org.java_websocket.handshake.ServerHandshake
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
    private var myUser: ParticipantRTC? = null
    private var localName = ""
    private val participantRTCList = mutableListOf<ParticipantRTC>()

    //    private val mediaStreamList = mutableListOf<MediaStream>()
    private var isAddTrackRunning = false
    private var isRemoveTrackRunning = false

    private var addMediaStream: StreamAsync? = null
    private var removeMediaStream: StreamAsync? = null

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
        rtcClient = BglobalRtcClient(
            application = this.application,
            observer = peerConnectionObserverImpl,
            callback = callback
        )
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        toggleAudio(true)
    }

    fun startCall(name: String) {
        localName = name
        myUser = ParticipantRTC(
            id = 0,
            name = localName,
            streamId = "",
            subIdList = mutableListOf()
        )
        myUser?.let {
            participantRTCList.add(0, it)
            rtcListener?.onUserListInRoom(participantRTCList)
        }
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

    private fun offerResponse(joinedSdp: String?) {
        /*
         * set remote với type == ANSWER để stream lên server
         */
        val session = SessionDescription(SessionDescription.Type.ANSWER, joinedSdp)
        rtcClient?.setRemoteSdpByAnswer(session)
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

    fun addLocalVideo(surface: SurfaceViewRenderer) {
        rtcClient?.addLocalVideo(surface)
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
        val request = handleModel.getPeer(transId = transIdByPeer, name = localName)
        socket?.sendMessageToSocket(request, "peer")
        transIdByPeer++
    }

    private val callback = object : BglobalRtcClient.Callback {
        override fun onSetLocalSdpOffer(state: BglobalRtcClient.State, sdp: SessionDescription) {
            onCreateOffer(sdp = sdp.description)
        }
    }

    private val peerConnectionObserverImpl = object : PeerConnectionObserverImpl() {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            rtcClient?.addIceCandidate(iceCandidate)
        }

        override fun onAddStream(track: MediaStream?) {
//            Log.d(TAG, "\n\nonAddStream: ${track?.id}")

            addMediaStream = StreamAsync(
                id = track?.id,
                track = track,
                type = RoomState.USER_JOIN
            )

            peer()
        }

        override fun onRemoveStream(track: MediaStream?) {
//            Log.d(TAG, "\n\nonRemoveStream: ${track?.id}")

//            removeMediaStream = StreamAsync(
//                id = track?.id,
//                track = track,
//                type = RoomState.USER_LEAVE
//            )

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
            offerResponse(response.getSdp())

            // TODO: có thể gây ra issue loop, kt log trên server
//            CoroutineScope(Dispatchers.IO).launch {
//                val request = handleModel.getPeer(username)
//                socket?.sendMessageToSocket(request, "peer")
//            }
        }

        override fun onPeer(participantDTOList: List<ParticipantDTO>) {
            val dtoList = participantDTOList.map {
                it.toParticipantRTC()
            }

            Log.d(TAG, "\nonPeer: $dtoList")

            val list = dtoList.toMutableList()
            if (addMediaStream != null) {
                list.forEachIndexed { i, v ->
//                    Log.d(TAG, "ccc1 : ${v.streamId}  ---  ${addMediaStream?.id}  ---  ${addMediaStream?.track}")
                    if (v.streamId == addMediaStream?.id) {
                        v.mediaStream = addMediaStream?.track
                        addMediaStream = null
                        return@forEachIndexed
                    }
                }
            }

            participantRTCList.replace(list)
            rtcListener?.onUserListInRoom(participantRTCList)
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
//                if (!isOnAddTrackRunning) {
//                val request = handleModel.getPeer(username)
//                socket?.sendMessageToSocket(request, "peer")
//                }
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
