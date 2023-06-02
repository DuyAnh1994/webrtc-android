package com.anhnd.webrtc.trios

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import java.net.URI
import java.net.URISyntaxException

class TriosSocketIO {

    private val TAG = "TriosSocketIO"
    private var socket: Socket? = null


    init {
        connect()
    }


    fun on(event: String, listener: Emitter.Listener) {
        socket?.on(event, listener)
    }

    fun emit(event: String) {
//        socket?.emit(event, request)
    }

    private fun connect() {
        try {
            val option = IO.Options()
            option.reconnection = false
            option.transports = arrayOf(WebSocket.NAME)
//            option.query = "side=client"

            val uri = URI.create("http://localhost:8080")
            socket = IO.socket(uri, option)

        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        socket?.connect()

        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "connect: EVENT_CONNECT")
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            it.forEach {cmd->
                Log.d(TAG, "connect: EVENT_CONNECT_ERROR $cmd")
            }
        }
        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "connect: EVENT_DISCONNECT")
        }

        socket?.on("") {

        }
    }
}
