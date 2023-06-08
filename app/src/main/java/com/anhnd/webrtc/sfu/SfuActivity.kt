package com.anhnd.webrtc.sfu

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.SfuActivityBinding
import com.anhnd.webrtc.utils.gone
import com.anhnd.webrtc.utils.initializeSurfaceView
import com.anhnd.webrtc.utils.observer
import com.anhnd.webrtc.utils.show
import com.bglobal.lib.publish.BglobalRtcListener
import com.bglobal.lib.publish.RtcManager
import org.webrtc.MediaStream

class SfuActivity : AppCompatActivity() {

    private lateinit var binding: SfuActivityBinding
    private val viewModel by viewModels<SfuViewModel>()
    private val roomAdapter by lazy { RoomAdapter() }
    private val rtcManager by lazy { RtcManager(application) }
    private val rtcListener = object : BglobalRtcListener {
        override fun onAddStream(mediaStream: MediaStream?) {
            if (!viewModel.isSameStreamDisplay(mediaStream)) {
                viewModel.insertMediaStream(mediaStream)
            }
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
            viewModel.removeMediaStream(mediaStream)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SfuActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rtcManager.build()
        rtcManager.addRtcListener(rtcListener)
        binding.svrLocal.initializeSurfaceView(rtcManager.getEglBase())

        roomAdapter.rtcManager = rtcManager

        binding.rvRoom.apply {
//            recycledViewPool.setMaxRecycledViews(RoomAdapter.PARTICIPANT_VIEW_TYPE, 20)
            adapter = roomAdapter
        }

        binding.sendCmd.setOnClickListener { doCall() }

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
        runOnUiThread {
            setWhoToCallLayoutGone()
            setCallLayoutVisible()
            rtcManager.startLocalVideo(binding.svrLocal)
            rtcManager.createOffer()
        }
    }

    private fun setCallLayoutVisible() {
        binding.callLayout.show()
    }

    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.gone()
    }
}
