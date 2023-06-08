package com.anhnd.webrtc.sfu

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anhnd.webrtc.sfu.domain.model.Participant
import com.anhnd.webrtc.utils.asLiveData
import com.anhnd.webrtc.utils.postSelf
import kotlinx.coroutines.launch
import org.webrtc.MediaStream

class SfuViewModel : ViewModel() {

    private val participantList = mutableListOf<Participant>(
//        Participant(index = 0, isLocal = true)
    )
    private val _participantListState = MutableLiveData(participantList)
    val participantListState = _participantListState.asLiveData()


    init {
//        mockData()
    }


    fun getItemById(id: String?): Participant? {
        return participantList.firstOrNull { it.id == id }
    }

    fun isSameStreamDisplay(mediaStream: MediaStream?): Boolean {
        participantList.forEach {
            if (it.mediaStream?.id == mediaStream?.id) {
                return true
            }
        }
        return false
    }

    fun insertMediaStream(mediaStream: MediaStream?) {
        val item = Participant(
            index = 1,
            id = mediaStream?.id,
            mediaStream = mediaStream
        )
        participantList.add(item)

        _participantListState.postSelf()
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

    private fun mockData() {
        viewModelScope.launch {
            for (i in 0..20) {
                participantList.add(Participant(index = i))
                if (i == 0) {
                    participantList[0].isLocal = true
                }
            }

            _participantListState.postValue(participantList)
        }
    }
}
