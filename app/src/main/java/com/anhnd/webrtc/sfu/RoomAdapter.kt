package com.anhnd.webrtc.sfu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.anhnd.webrtc.databinding.ParticipantItemBinding
import com.anhnd.webrtc.sfu.domain.model.Participant
import com.anhnd.webrtc.utils.initializeSurfaceView
import com.bglobal.lib.publish.RtcController

class RoomAdapter : RecyclerView.Adapter<RoomAdapter.ParticipantVH>() {

    companion object {
        private const val TAG = "RoomAdapter"
        const val PARTICIPANT_VIEW_TYPE = 1
    }

    var rtcManager: RtcController? = null
    private var currentList = mutableListOf<Participant>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantVH {
        val binding = ParticipantItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantVH(binding)
    }

    override fun getItemCount() = currentList.count()

    override fun getItemViewType(position: Int): Int {
        return PARTICIPANT_VIEW_TYPE
    }

    override fun onBindViewHolder(holder: ParticipantVH, position: Int) {
        holder.onBind(currentList[holder.adapterPosition])
    }

    fun submitList(list: MutableList<Participant>) {
        currentList.clear()
        currentList.addAll(list)
        notifyDataSetChanged()
    }

    inner class ParticipantVH(private val binding: ParticipantItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            rtcManager?.getEglBase()?.let {
                binding.svrUser.initializeSurfaceView(it)
            }
        }

        fun onBind(data: Participant) {
            if (adapterPosition == 0) {
                rtcManager?.startLocalVideo(binding.svrUser)
            }

            binding.tvIndex.text = "$adapterPosition"
            data.addSink(binding.svrUser)
        }
    }
}

class ParticipantDiffCallback() : DiffUtil.ItemCallback<Participant>() {

    override fun areItemsTheSame(oldItem: Participant, newItem: Participant): Boolean {
        return oldItem.name == newItem.name

    }

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean {
        return oldItem.name == newItem.name
    }
}
