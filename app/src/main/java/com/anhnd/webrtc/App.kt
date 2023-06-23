package com.anhnd.webrtc

import android.app.Application
import com.anhnd.webrtc.utils.setApplication

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        setApplication(this)
    }
}
