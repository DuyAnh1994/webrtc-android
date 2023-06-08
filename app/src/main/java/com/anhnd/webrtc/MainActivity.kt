package com.anhnd.webrtc

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.MainActivityBinding
import com.anhnd.webrtc.p2p.P2PActivity
import com.anhnd.webrtc.sfu.SfuActivity
import com.anhnd.webrtc.utils.toast
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        viewModel.getAll()

        binding.btnP2P.setOnClickListener {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ).request { allGranted, _, _ ->
                    if (allGranted) {
                        val intent = Intent(this, P2PActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "you should accept all permissions", Toast.LENGTH_LONG).show()
                    }
                }
        }
        binding.btnSFU.setOnClickListener {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ).request { allGranted, _, _ ->
                    if (allGranted) {
                        val intent = Intent(this, SfuActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "you should accept all permissions", Toast.LENGTH_LONG).show()
                    }
                }
        }
        binding.btnMCU.setOnClickListener {
            toast("webrtc by mcu")
        }
    }
}
