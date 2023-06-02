package com.anhnd.webrtc.p2p

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anhnd.webrtc.databinding.P2pActivityBinding
import com.permissionx.guolindev.PermissionX

class P2PActivity : AppCompatActivity() {

    private lateinit var binding: P2pActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = P2pActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.enterBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ).request { allGranted, _, _ ->
                    if (allGranted) {
                        val intent = Intent(this, P2PCallActivity::class.java).apply {
                            putExtra("username", binding.username.text.toString())
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "you should accept all permissions", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
