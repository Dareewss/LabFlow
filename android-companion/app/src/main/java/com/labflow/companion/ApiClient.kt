package com.labflow.companion

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    fun create(host: String, port: String): LabFlowApi {
        val rawHost = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val hostWithoutPath = rawHost.substringBefore('/')
        val hostParts = hostWithoutPath.split(':', limit = 2)
        val cleanHost = hostParts[0].trim()
        require(cleanHost.isNotBlank()) { "Introdu IP-ul PC-ului." }
        val cleanPort = when {
            port.trim().isNotBlank() -> port.trim()
            hostParts.size == 2 -> hostParts[1].trim()
            else -> "8080"
        }
        val portNumber = cleanPort.toIntOrNull()
        require(portNumber != null && portNumber in 1..65535) { "Port invalid." }
        val baseUrl = "http://$cleanHost:$cleanPort/"
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LabFlowApi::class.java)
    }
}
