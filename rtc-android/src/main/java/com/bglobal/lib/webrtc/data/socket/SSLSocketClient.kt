package com.bglobal.lib.webrtc.data.socket

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SSLSocketClient {
    fun getSSLSocketFactory(): SSLSocketFactory {
        return try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, getTrustManager(), SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun getTrustManager(): Array<TrustManager> {
        return arrayOf(getX509TrustManager())
    }

    fun getX509TrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {

            }

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }

    fun getHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { _, _ -> true }
    }
}
