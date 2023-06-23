package com.anhnd.webrtc.sfu

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.anhnd.webrtc.databinding.SfuActivityBinding
import com.anhnd.webrtc.utils.initializeSurfaceView
import com.anhnd.webrtc.utils.observer
import com.bglobal.lib.publish.BglobalRtcListener
import com.bglobal.lib.publish.ParticipantRTC
import com.bglobal.lib.publish.WebRTCController

class SfuActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SfuActivity"
    }

    private lateinit var binding: SfuActivityBinding
    private val viewModel by viewModels<SfuViewModel>()
    private val roomAdapter by lazy { RoomAdapter() }
    private val streamIdList = mutableListOf<String?>()

    private val rtcManager by lazy { WebRTCController(application) }
    private val rtcListener = object : BglobalRtcListener {
        override fun onUserListInRoom(totalList: List<ParticipantRTC>) {
//            Log.d(TAG, "\n\n onUserListInRoom   total = ${totalList.count()} ----------------------------------------------")
//            totalList.forEach {
//                Log.d(TAG, "onUserListInRoom: id=${it.id} name=${it.name} streamId=${it.streamId} ms=${it.mediaStream}")
//            }

            viewModel.replaceParticipantList(totalList)
        }

//        override fun onAddStream(track: MediaStream?) {
//            streamIdList.add(track?.id)
//
//            val streamIdSB = StringBuilder("onAddTrack:\n")
//
//            streamIdList.forEachIndexed { i, v ->
//                streamIdSB.append("\n $i. $v")
//            }
//
//            runOnUiThread {
//                binding.tvLog.text = streamIdSB.toString()
//            }
//        }
//
//        override fun onRemoveStream(track: MediaStream?) {
////            viewModel.removeMediaStream(mediaStream)
//
//            streamIdList.remove(track?.id)
//
//            val streamIdSB = StringBuilder("onAddTrack:\n")
//
//            streamIdList.forEachIndexed { i, v ->
//                streamIdSB.append("\n $i. $v")
//            }
//
//            runOnUiThread {
//                binding.tvLog.text = streamIdSB.toString()
//            }
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SfuActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rtcManager.build()
        rtcManager.addRtcListener(rtcListener)

        roomAdapter.rtcManager = rtcManager

        binding.rvRoom.apply {
            layoutManager = GridLayoutManager(this@SfuActivity, 5)
            adapter = roomAdapter
        }

        binding.ivCall.setOnClickListener { doCall() }

        observer(viewModel.localName) {
            binding.tvName.text = String.format("local name:[ $it ]")
        }

        binding.ivCheckPartiList.setOnClickListener {
            rtcManager.peer()
        }

        binding.endCallButton.setOnClickListener {
            rtcManager.endCall()
            finish()
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
        rtcManager.addLocalVideo(binding.svrLocal)
    }

    override fun onDestroy() {
        rtcManager.removeRctListener()
        super.onDestroy()
    }

    private fun doCall() {
        rtcManager.startCall(viewModel.getLocalName())
    }
}
