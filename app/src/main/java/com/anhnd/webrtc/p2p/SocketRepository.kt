package com.anhnd.webrtc.p2p

import android.util.Log
import com.anhnd.webrtc.p2p.models.MessageModel
import com.anhnd.webrtc.utils.TAG
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class SocketRepository(private val messageInterface: NewMessageInterface) {
    private var webSocket: WebSocketClient? = null
    private var userName: String? = null
    private val gson = Gson()

    fun initSocket(username: String) {
        userName = username

        val wsUrl = "ws://10.0.2.2:3000"
//        val wsUrl = "ws://192.168.1.55:3000"

        webSocket = object : WebSocketClient(URI(wsUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessageToSocket(
                    MessageModel("store_user", username, null, null)
                )
            }

            override fun onMessage(message: String?) {
                try {
                    Log.d(TAG, "receiver json: $message")
                    messageInterface.onNewMessage(gson.fromJson(message, MessageModel::class.java))

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG, "onClose: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.d(TAG, "onError: $ex")
            }

        }
        webSocket?.connect()

    }

    fun sendMessageToSocket(message: MessageModel) {
        try {
            val json = Gson().toJson(message)
            webSocket?.send(json)
            Log.d(TAG, "send json: $json")
        } catch (e: Exception) {
            Log.d(TAG, "sendMessageToSocket: $e")
        }
    }
}
