package com.anhnd.webrtc.sfu

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anhnd.webrtc.databinding.ParticipantItemBinding
import com.anhnd.webrtc.sfu.domain.model.Participant
import com.anhnd.webrtc.utils.initializeSurfaceView
import com.bglobal.lib.publish.RtcManager

class RoomAdapter : ListAdapter<Participant, RoomAdapter.ParticipantVH>(ParticipantDiffCallback()) {

    companion object {
        private const val TAG = "RoomAdapter"
        const val PARTICIPANT_VIEW_TYPE = 1
    }

    var rtcManager: RtcManager? = null
    private val cacheMap = mutableMapOf<String?, Participant>()

    inner class ParticipantVH(private val binding: ParticipantItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            rtcManager?.getEglBase()?.let {
                binding.svrUser.initializeSurfaceView(it)
            }
        }

        fun onBind(data: Participant) {
//            Log.d(TAG, "onBind: id=${data.id} --- ${isExistInMap(data.id)}")
//            if (!isExistInMap(data.id)) {
//                cacheMap[data.id] = data
//                data.addSink(binding.svrUser)
//            } else {
//                binding.tvIndex.text = "${data.index}"
//                data.addSink(binding.svrUser)
//            }


            binding.tvIndex.text = "${data.index}"
            data.addSink(binding.svrUser)
        }

        private fun isExistInMap(id: String?): Boolean {
            return cacheMap.containsKey(id)
        }
    }

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
}

class ParticipantDiffCallback() : DiffUtil.ItemCallback<Participant>() {

    override fun areItemsTheSame(oldItem: Participant, newItem: Participant): Boolean {
        return oldItem.id == newItem.id

    }

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean {
        return oldItem.mediaStream?.id == newItem.mediaStream?.id
    }
}
