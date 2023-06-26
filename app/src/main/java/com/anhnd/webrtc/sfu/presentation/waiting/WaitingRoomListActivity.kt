package com.anhnd.webrtc.sfu.presentation.waiting

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.WaitingRoomListActivityBinding
import com.anhnd.webrtc.sfu.presentation.call.SfuActivity

class WaitingRoomListActivity : AppCompatActivity() {

    private lateinit var binding: WaitingRoomListActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WaitingRoomListActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvRoomTest1.setOnClickListener {
            navigateTo(SfuActivity::class.java, "room_id", "1")
        }

        binding.tvRoomTest2.setOnClickListener {
            navigateTo(SfuActivity::class.java, "room_id", "2")
        }

        binding.tvRoomTest3.setOnClickListener {
            navigateTo(SfuActivity::class.java, "room_id", "3")
        }
    }

    fun navigateTo(clazz: Class<out AppCompatActivity>, key: String? = null, value: String? = null) {
        val intent = Intent(this, clazz)
        intent.putExtra(key, value)
        startActivity(intent)
    }
}
