package com.anhnd.webrtc.trios

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.anhnd.webrtc.databinding.ParticipantItemBinding
import com.anhnd.webrtc.trios.domain.model.Participant
import com.anhnd.webrtc.utils.initializeSurfaceView
import com.bglobal.lib.publish.RtcManager

class RoomAdapter : RecyclerView.Adapter<RoomAdapter.ParticipantVH>() {

    companion object {
        private const val TAG = "RoomAdapter"
    }

    var rtcManager: RtcManager? = null
    private val participantList = mutableListOf<Participant>()

    inner class ParticipantVH(private val binding: ParticipantItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            rtcManager?.getEglBase()?.let {
                binding.svrUser.initializeSurfaceView(it)
            }
        }

        fun onBind(data: Participant) {
//            if (data.isLocal && !data.isLoaded) {
//                rtcManager?.startLocalVideo(binding.svrUser)
//            }
            data.addSink(binding.svrUser)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantVH {
        val binding = ParticipantItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantVH(binding)
    }

    override fun getItemCount() = participantList.count()

    override fun onBindViewHolder(holder: ParticipantVH, position: Int) {
        holder.onBind(participantList[holder.adapterPosition])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: MutableList<Participant>) {
        participantList.clear()
        participantList.addAll(list)
        notifyDataSetChanged()
    }
}
