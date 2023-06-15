package com.anhnd.webrtc.sfu

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anhnd.webrtc.sfu.domain.model.Participant
import com.anhnd.webrtc.utils.asLiveData
import com.anhnd.webrtc.utils.postSelf
import com.bglobal.lib.publish.ParticipantRTC
import com.bglobal.lib.utils.replace
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
            streamIdOrigin = "",
            isLocal = true
        )
        participantList.add(item)
        _participantListState.postSelf()
    }

    fun replaceParticipantList(list: List<ParticipantRTC>) {
        val newList = list.map {
            Participant(
                id = it.id,
                name = it.name,
                streamIdOrigin = it.streamIdOrigin,
                streamIdSecondary = it.streamIdSecondary,
                mediaStream = it.mediaStream
            )
        }
        _participantListState.value?.replace(newList)
        _participantListState.postSelf()
    }

    fun userJoinRoom(user: ParticipantRTC) {
        viewModelScope.launch {
            val item = Participant(
                id = user.id,
                name = user.name,
                streamIdOrigin = user.streamIdOrigin,
                streamIdSecondary = user.streamIdSecondary
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
//        val index = participantList.indexOfFirst {
////            Log.d(TAG, "updateMediaStream  1: streamId=[${it.streamId}]   mediaStreamId=[${mediaStream?.id}]")
////            it.streamIdSecondary.contains(mediaStream?.id)
//
//            containsStreamIdSecondary(it.streamIdSecondary, mediaStream?.id)
//        }

        participantList.forEach {
            it.streamIdSecondary.forEach {subId->
                Log.d(TAG, "updateMediaStream: $subId   ${mediaStream?.id}   ${subId==mediaStream?.id}")
            }
        }

//        Log.d(TAG, "updateMediaStream index: $index")

//        if (index in 0..participantList.lastIndex) {
//            participantList[index].mediaStream = mediaStream
//        }
    }

    private fun containsStreamIdSecondary(list: List<String>, id: String?): Boolean {
        list.forEach {
            Log.d(TAG, "containsStreamIdSecondary: sub_id=[$it]     id=[$id]")

            if (it == id) return true
        }
        return false
    }

    fun addMediaStream(stream: MediaStream?) {
        try {
            participantList.add(Participant(
                id = 0,
                name = stream?.id ?: "",
                streamIdOrigin = stream?.id ?: "",
                mediaStream = stream
            ))
            _participantListState.postSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getItemById(id: String?): Participant? {
        return participantList.firstOrNull { it.streamIdOrigin == id }
    }

    fun isSameStreamDisplay(mediaStream: MediaStream?): Boolean {
        participantList.forEach {
            if (it.streamIdSecondary.contains(mediaStream?.id)) {
                return true
            }
        }
        return false
    }

    fun removeMediaStream(mediaStream: MediaStream?) {
        val item = getItemById(mediaStream?.id)
        participantList.remove(item)
        _participantListState.postSelf()
    }

    fun check() {
        _participantListState.value?.forEach {
            Log.d("anhnd", "check: ${it.mediaStream?.id}")
        }
    }
}
