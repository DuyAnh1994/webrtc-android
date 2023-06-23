package com.anhnd.webrtc.sfu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.anhnd.webrtc.R
import com.anhnd.webrtc.databinding.ParticipantGridItemBinding
import com.anhnd.webrtc.sfu.domain.model.Participant
import com.anhnd.webrtc.utils.initializeSurfaceView
import com.bglobal.lib.publish.WebRTCController

class RoomAdapter : RecyclerView.Adapter<RoomAdapter.ParticipantVH>() {
//class RoomAdapter : ListAdapter<Participant, RoomAdapter.ParticipantVH>(ParticipantItemCallback()) {

    companion object {
        private const val TAG = "RoomAdapter"
        const val PARTICIPANT_VIEW_TYPE = 1
    }

    var rtcManager: WebRTCController? = null
    private var currentList = mutableListOf<Participant>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantVH {
//        val binding = ParticipantItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val binding = ParticipantGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantVH(binding)
    }

    override fun getItemCount() = currentList.count()

    override fun getItemViewType(position: Int): Int {
        return PARTICIPANT_VIEW_TYPE
    }

    override fun onBindViewHolder(holder: ParticipantVH, position: Int) {
        holder.onBind(currentList[holder.adapterPosition])
    }

//    fun submitList(list: MutableList<Participant>) {
//        currentList.clear()
//        currentList.addAll(list)
//        notifyDataSetChanged()
//    }

    fun submitList(newData: MutableList<Participant>) {
        val newList = newData.toMutableList()
        val callback = PaymentDiffCallback(currentList, newList)
        val diffResult = DiffUtil.calculateDiff(callback)
        this.currentList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    //    inner class ParticipantVH(private val binding: ParticipantItemBinding) : RecyclerView.ViewHolder(binding.root) {
    inner class ParticipantVH(private val binding: ParticipantGridItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private var initialize = false

        init {
            try {
                if (!initialize) {
                    rtcManager?.getEglBase()?.let {
                        binding.svrUser.initializeSurfaceView(it)
                    }
                    initialize = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun onBind(data: Participant) {
            if (data.isLocal) {
                rtcManager?.addLocalVideo(binding.svrUser)
            }

            binding.apply {
                tvName.text = String.format("--${data.name}--")
//                tvMediaStreamInstance.text = String.format("ms ins: ${data.mediaStream}")
//                tvStreamId.text = String.format("streamId: ${data.streamId}")
//                tvSubIdList.text = data.getStreamIdSecondary()
                cvParti.strokeColor = if (data.isLocal) {
                    ContextCompat.getColor(binding.root.context, R.color.red)
                } else {
                    ContextCompat.getColor(binding.root.context, R.color.transparent)
                }
            }
            data.addSink(binding.svrUser)
        }
    }
}

class ParticipantItemCallback : DiffUtil.ItemCallback<Participant>() {

    override fun areItemsTheSame(oldItem: Participant, newItem: Participant): Boolean {
        return oldItem.id == newItem.id

    }

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean {
        return oldItem.name == newItem.name
    }
}

class PaymentDiffCallback(
    private val oldData: List<Participant>,
    private val newData: List<Participant>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldData.count()

    override fun getNewListSize() = newData.count()

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

        return getOldItem(oldItemPosition).id == getNewItem(newItemPosition).id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

        return getOldItem(oldItemPosition).name == getNewItem(newItemPosition).name
    }

    private fun getOldItem(position: Int) = oldData[position]

    private fun getNewItem(position: Int) = newData[position]
}
