package com.anhnd.webrtc.sfu

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.SfuActivityBinding
import com.anhnd.webrtc.utils.initializeSurfaceView
import com.anhnd.webrtc.utils.observer
import com.bglobal.lib.publish.BglobalRtcListener
import com.bglobal.lib.publish.ParticipantRTC
import com.bglobal.lib.publish.WebRTCController
import org.webrtc.MediaStream

class SfuActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SfuActivity"
    }

    private lateinit var binding: SfuActivityBinding
    private val viewModel by viewModels<SfuViewModel>()
    private val roomAdapter by lazy { RoomAdapter() }
    private val streamIdList = mutableListOf<String?>()
//    private val streamIdSB = StringBuilder("onAddTrack:\n")

    private val rtcManager by lazy { WebRTCController(application) }
    private val rtcListener = object : BglobalRtcListener {
        override fun onUserListInRoom(totalList: List<ParticipantRTC>) {
            Log.d(TAG, "\n\n onUserListInRoom   total = ${totalList.count()} ----------------------------------------------")
            totalList.forEach {
                Log.d(TAG, "onUserListInRoom: id=${it.id} name=${it.name} streamId=${it.streamId} ms=${it.mediaStream}")
            }

            viewModel.replaceParticipantList(totalList)
        }

        override fun onUserJoinRoom(userJoin: ParticipantRTC) {
            Log.d(TAG, "onUserJoinRoom: name=[${userJoin.name}]   streamId=[${userJoin.streamId}]")
//            viewModel.userJoinRoom(userJoin)
        }

        override fun onUserLeaveRoom(userLeave: ParticipantRTC) {
//            viewModel.userLeaveRoom(userLeave)
        }


        override fun onAddStream(mediaStream: MediaStream?) {
//            Log.d(TAG, "onAddStream: ${mediaStream?.id}")

//            rtcManager.peer()
//            if (!viewModel.isSameStreamDisplay(mediaStream)) {
//                viewModel.updateMediaStream(mediaStream)
//            } else {
//            viewModel.addMediaStream(mediaStream)
//            }

//            viewModel.addMediaStream(mediaStream)

            streamIdList.add(mediaStream?.id)

            val streamIdSB = StringBuilder("onAddTrack:\n")

            streamIdList.forEachIndexed { i, v ->
                streamIdSB.append("\n $i. $v")
            }

            runOnUiThread {
                binding.tvMediaStreamCallback.text = streamIdSB.toString()
            }
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
//            viewModel.removeMediaStream(mediaStream)

            streamIdList.remove(mediaStream?.id)

            val streamIdSB = StringBuilder("onAddTrack:\n")

            streamIdList.forEachIndexed { i, v ->
                streamIdSB.append("\n $i. $v")
            }

            runOnUiThread {
                binding.tvMediaStreamCallback.text = streamIdSB.toString()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SfuActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rtcManager.build()
        rtcManager.addRtcListener(rtcListener)

        roomAdapter.rtcManager = rtcManager

        binding.rvRoom.apply {
            adapter = roomAdapter
        }

        binding.ivCall.setOnClickListener { doCall() }

        binding.ivCheckPartiList.setOnClickListener {
            rtcManager.peer()
        }

        binding.endCallButton.setOnClickListener {
            viewModel.check()
        }

        binding.btnSwitchCamera.setOnClickListener {
            rtcManager.switchCamera()
        }

        rtcManager.toggleAudio(false)

        binding.ivMute.setOnClickListener {
//            rtcManager.toggleAudio(true)

//            rtcManager.updateOffer()
        }

        observer(viewModel.participantListState) {
            roomAdapter.submitList(it)
        }

        binding.svrLocal.initializeSurfaceView(rtcManager.getEglBase())
        rtcManager.startLocalVideo(binding.svrLocal)
    }

    override fun onDestroy() {
        rtcManager.removeRctListener()
        super.onDestroy()
    }

    private fun doCall() {
        rtcManager.startCall(getRandomString())
    }

    private fun getRandomString(): String {
        val allowedChars = ('a'..'z')
        return (1..3)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
