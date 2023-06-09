package com.anhnd.webrtc.sfu

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.SfuActivityBinding
import com.anhnd.webrtc.utils.observer
import com.bglobal.lib.publish.BglobalRtcListener
import com.bglobal.lib.publish.ParticipantRtcModel
import com.bglobal.lib.publish.RtcManager
import org.webrtc.MediaStream

class SfuActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SfuActivity"
    }

    private lateinit var binding: SfuActivityBinding
    private val viewModel by viewModels<SfuViewModel>()
    private val roomAdapter by lazy { RoomAdapter() }



    private val rtcManager by lazy { RtcManager(application) }
    private val rtcListener = object : BglobalRtcListener {
        override fun onUserListInRoom(totalList: List<ParticipantRtcModel>) {
            Log.d(TAG, "\n\n onUserListInRoom   total = ${totalList.count()} ----------------------------------------------")
            totalList.forEach {
                Log.d(TAG, "onUserListInRoom: id=${it.id} name=${it.name} streamId=${it.streamId} ")
            }
        }

        override fun onUserJoinRoom(userJoin: ParticipantRtcModel) {
            viewModel.userJoinRoom(userJoin)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            if (!viewModel.isSameStreamDisplay(mediaStream)) {
                viewModel.updateMediaStream(mediaStream)
            }
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
//            viewModel.removeMediaStream(mediaStream)
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

        binding.endCallButton.setOnClickListener {
            viewModel.check()
        }

        observer(viewModel.participantListState) {
            roomAdapter.submitList(it)
        }
    }

    override fun onDestroy() {
        rtcManager.removeRctListener()
        super.onDestroy()
    }

    private fun doCall() {
        rtcManager.createOffer()
    }
}
