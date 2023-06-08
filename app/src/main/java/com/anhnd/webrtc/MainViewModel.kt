package com.anhnd.webrtc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    init {

    }

    fun getAll() {
        viewModelScope.launch {
            val list = mutableListOf<String>()
            delay(1000)
            val a = async { fetch("a", 5, 500) }
            val b = async { fetch("b", 5, 700) }
            val c = async { fetch("c", 5, 900) }


            val aList = a.await()
            val bList = b.await()
            val cList = c.await()
//
            list.addAll(aList)
            list.addAll(bList)
            list.addAll(cList)
            Log.d(TAG, "getAll: ${list.count()}")
        }
    }


    private suspend fun fetch(char: String, size: Int, delay: Long): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until size) {
            delay(delay)
            Log.d(TAG, "string - $char - $i")
            list.add("string - $char - $i")
        }
        return list
    }
}
