package com.bglobal.lib.webrtc.data

class RtcException : Exception {

    companion object {
        /**
         * sdk code
         */
        const val UNKNOWN_ERROR = -1001


        /**
         * server code
         */
        const val SUCCESS = 2000
    }

    var code: Int
    var msg: String? = null
    var payload: Any? = null

    constructor() : super() {
        this.code = UNKNOWN_ERROR
    }

    constructor(code: Int) : super() {
        this.code = code
    }

    constructor(message: String?) : super(message) {
        this.code = UNKNOWN_ERROR
        this.msg = message
    }

    constructor(
        code: Int,
        message: String?
    ) : super(message) {
        this.code = code
        this.msg = message
    }

    constructor(
        code: Int,
        message: String?,
        throwable: Throwable?
    ) : super(message, throwable) {
        this.code = code
        this.msg = message
    }

    constructor(
        code: Int,
        message: String?,
        throwable: Throwable?,
        payload: Any?
    ) : super(message, throwable) {
        this.code = code
        this.msg = message
        this.payload = payload
    }
}
