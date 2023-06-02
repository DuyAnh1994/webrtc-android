package com.anhnd.webrtc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.MainActivityBinding
import com.anhnd.webrtc.p2p.P2PActivity
import com.anhnd.webrtc.trios.SfuActivity
import com.anhnd.webrtc.utils.toast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnP2P.setOnClickListener {
            val intent = Intent(this, P2PActivity::class.java)
            startActivity(intent)
        }
        binding.btnSFU.setOnClickListener {
            val intent = Intent(this, SfuActivity::class.java)
            startActivity(intent)
        }
        binding.btnMCU.setOnClickListener {
            toast("webrtc by mcu")
        }
    }
}
