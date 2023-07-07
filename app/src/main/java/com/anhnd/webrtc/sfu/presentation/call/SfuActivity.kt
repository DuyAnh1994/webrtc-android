package com.anhnd.webrtc.sfu.presentation.call

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.R
import com.anhnd.webrtc.databinding.ParticipantGridItemBinding
import com.anhnd.webrtc.databinding.SfuActivityBinding
import com.anhnd.webrtc.utils.addSink
import com.anhnd.webrtc.utils.getAppColor
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SfuActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val id = intent.getStringExtra("room_id") ?: viewModel.getRoomId()
        viewModel.roomId.value = id
        rtcManager.build(viewModel.getRoomId())
        rtcManager.addRtcListener(rtcListener)

        roomAdapter.rtcManager = rtcManager

//        binding.rvRoom.apply {
//            layoutManager = GridLayoutManager(this@SfuActivity, 4)
//            adapter = roomAdapter
//        }


        binding.ivCall.setOnClickListener { doCall() }

        observer(viewModel.localName) {
            binding.tvName.text = String.format("local name:[ $it ]")
        }

        observer(viewModel.roomId) {
            val resId = when (it) {
                "1" -> R.color.red
                "2" -> R.color.yellow
                "3" -> R.color.blue
                else -> R.color.white
            }
            binding.tvRoomId.setBackgroundColor(getAppColor(resId))
            binding.tvRoomId.text = String.format("Room $it")
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
//            roomAdapter.submitList(it)

            Log.d(TAG, "onCreate: participants count=[${it.count()}]")

            it.forEach { parti ->
                val itemView = createVideoView(binding.glRoom)
                itemView.svrUser.initializeSurfaceView(rtcManager.getEglBase())
                itemView.svrUser.addSink(parti.getVideoTrack())
                binding.glRoom.addView(itemView.root)
            }
        }

        binding.svrLocal.initializeSurfaceView(rtcManager.getEglBase())
        rtcManager.initLocalVideo(binding.svrLocal)
        rtcManager.turnOnCamera()
    }

    private fun createVideoView(parent: ViewGroup): ParticipantGridItemBinding {
        return ParticipantGridItemBinding.inflate(
            LayoutInflater.from(this),
            parent,
            false
        )
    }

    override fun onDestroy() {
        rtcManager.removeRctListener()
        super.onDestroy()
    }

    private fun doCall() {
        rtcManager.startCall(viewModel.getLocalName())
    }

    private val rtcListener = object : BglobalRtcListener {
        override fun onConnect(url: String, code: Int, msg: String) {
            binding.tvUrlWS.text = String.format("- url=[$url]\n\n- code=[$code]\n\n- msg=[$msg]")
        }

        override fun onDisconnect(code: Int, reason: String?, remote: Boolean) {
            binding.tvUrlWS.text = String.format("- code=[$code]\n\n- reason=[$reason]\n\n- remote=[$remote]")
        }

        override fun onError(exception: Exception?) {
            binding.tvUrlWS.text = String.format("- ex msg=[${exception?.message}]")
        }

        override fun onUserListInRoom(totalList: List<ParticipantRTC>) {
            Log.d(TAG, "\n\n onUserListInRoom   total = ${totalList.count()} ----------------------------------------------")
            totalList.forEach {
                Log.d(TAG, "onUserListInRoom: id=${it.id} name=${it.name} streamId=${it.streamId} ms=${it.mediaStream}")
            }

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
}
