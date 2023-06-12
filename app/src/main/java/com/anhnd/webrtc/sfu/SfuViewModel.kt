package com.anhnd.webrtc.sfu

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anhnd.webrtc.sfu.domain.model.Participant
import com.anhnd.webrtc.utils.asLiveData
import com.anhnd.webrtc.utils.postSelf
import com.bglobal.lib.publish.ParticipantRTC
import kotlinx.coroutines.launch
import org.webrtc.MediaStream

class SfuViewModel : ViewModel() {

    private val TAG = "SfuViewModel"

    private val participantList = mutableListOf<Participant>()
    private val _participantListState = MutableLiveData(participantList)
    val participantListState = _participantListState.asLiveData()

    init {
        initLocalStream()
    }

    private fun initLocalStream() {
        val item = Participant(
            id = 0,
            name = "",
            streamId = "",
            isLocal = true
        )
        participantList.add(item)
        _participantListState.postSelf()
    }

    fun userJoinRoom(user: ParticipantRTC) {
        viewModelScope.launch {
            val item = Participant(
                id = user.id,
                name = user.name,
                streamId = user.streamId
            )
            participantList.add(item)
            _participantListState.postSelf()
        }
    }

    fun userLeaveRoom(user: ParticipantRTC) {
        viewModelScope.launch {
            val item = participantList.find { it.id == user.id }
            participantList.remove(item)
            _participantListState.postSelf()
        }
    }

    fun updateMediaStream(mediaStream: MediaStream?) {
        val index = participantList.indexOfFirst {
            Log.d(TAG, "updateMediaStream  1: streamId=[${it.streamId}]   mediaStreamId=[${mediaStream?.id}]")
            it.streamId == mediaStream?.id
        }

        Log.d(TAG, "updateMediaStream index: $index")

        if (index in 0..participantList.lastIndex) {
            participantList[index].mediaStream = mediaStream
        }

        _participantListState.postSelf()
    }

    private fun getItemById(id: Int): Participant? {
        return participantList.firstOrNull { it.id == id }
    }

    fun isSameStreamDisplay(mediaStream: MediaStream?): Boolean {
        participantList.forEach {
            if (it.streamId == mediaStream?.id) {
                return true
            }
        }
        return false
    }

    fun removeMediaStream(mediaStream: MediaStream?) {
        val item = getItemById(0)
        participantList.remove(item)
        _participantListState.postSelf()
    }

    fun check() {
        _participantListState.value?.forEach {
            Log.d("anhnd", "check: ${it.mediaStream?.id}")
        }
    }
}
