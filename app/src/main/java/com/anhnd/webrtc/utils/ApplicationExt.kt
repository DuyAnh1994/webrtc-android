package com.anhnd.webrtc.utils

import android.app.Application

private var application: Application? = null

fun setApplication(context: Application) {
    application = context
}

fun getApplication() = application ?: throw RuntimeException("Application context mustn't null")

const val BOOLEAN_DEFAULT = false
const val INT_DEFAULT = 0
const val LONG_DEFAULT = 0L
const val DOUBLE_DEFAULT = 0.0
const val FLOAT_DEFAULT = 0f
const val STRING_DEFAULT = ""
