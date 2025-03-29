package com.mitra.ai.xyz.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.IOException
import java.io.BufferedReader
import java.io.InputStreamReader

class SSEInterceptor(private val listener: SSEListener) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.isSuccessful && response.body != null && 
            response.header("Content-Type")?.contains("text/event-stream") == true) {
            
            // Get the original response body
            val responseBody = response.body!!
            val source = responseBody.source()
            
            // Create a new buffer to hold the response
            val buffer = Buffer()
            
            // Start reading the stream in a separate thread
            Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                    var line: String?
                    
                    while (reader.readLine().also { line = it } != null) {
                        if (line?.startsWith("data: ") == true) {
                            val data = line!!.substring(6)
                            if (data == "[DONE]") {
                                listener.onComplete()
                                break
                            } else {
                                listener.onEvent(data)
                            }
                        }
                    }
                } catch (e: Exception) {
                    listener.onFailure(e)
                }
            }.start()

            // Return a modified response that won't be buffered
            return response.newBuilder()
                .body(buffer.readByteString().toResponseBody(response.body!!.contentType()))
                .build()
        }
        
        return response
    }
}

interface SSEListener {
    fun onEvent(data: String)
    fun onFailure(t: Throwable)
    fun onComplete()
} 