package com.anhnd.webrtc.trios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.anhnd.webrtc.databinding.ParticipantItemBinding
import com.anhnd.webrtc.trios.domain.model.Participant
import com.anhnd.webrtc.utils.initializeSurfaceView
import org.webrtc.EglBase

class RoomAdapter : RecyclerView.Adapter<RoomAdapter.ParticipantVH>() {

    private val participantList = mutableListOf<Participant>()

    inner class ParticipantVH(private val binding: ParticipantItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private val eglContext = EglBase.create()

        init {
            binding.svrRoom.initializeSurfaceView(eglContext)
        }

        fun onBind(data: Participant) {

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
}
