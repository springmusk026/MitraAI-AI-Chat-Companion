package com.mitra.ai.xyz.data.remote

import okhttp3.*
import okio.IOException
import okio.Buffer
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class EventSource private constructor(
    private val client: OkHttpClient,
    private val request: Request,
    private val listener: EventSourceListener
) {
    private var call: Call? = null
    private var isClosed = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    fun connect() {
        if (isClosed) {
            throw IllegalStateException("EventSource is closed")
        }

        scope.launch {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    listener.onFailure(IOException("Unexpected response code: ${response.code}"))
                    return@launch
                }

                val source = response.body?.source() ?: run {
                    listener.onFailure(IOException("Response body is null"))
                    return@launch
                }

                val buffer = Buffer()
                while (!isClosed) {
                    val line = try {
                        source.readUtf8Line()
                    } catch (e: IOException) {
                        if (!isClosed) {
                            listener.onFailure(e)
                        }
                        break
                    } ?: break // End of stream

                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)
                        if (data == "[DONE]") {
                            listener.onComplete()
                            break
                        } else {
                            listener.onEvent(data)
                        }
                    }
                }

                response.close()
            } catch (e: Exception) {
                if (!isClosed) {
                    listener.onFailure(e)
                }
            }
        }
    }

    fun close() {
        isClosed = true
        call?.cancel()
        scope.cancel()
    }

    companion object {
        fun Builder(
            client: OkHttpClient,
            request: Request
        ): Builder = Builder(client, request)
    }

    class Builder(
        private val client: OkHttpClient,
        private val request: Request
    ) {
        private var listener: EventSourceListener? = null

        fun listener(listener: EventSourceListener): Builder {
            this.listener = listener
            return this
        }

        fun build(): EventSource {
            if (listener == null) {
                throw NullPointerException("EventSourceListener must not be null")
            }
            
            // Configure client for SSE
            val clientBuilder = client.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)

            // Ensure correct headers for SSE
            val requestBuilder = request.newBuilder()
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")

            return EventSource(
                clientBuilder.build(),
                requestBuilder.build(),
                listener!!
            )
        }
    }
}

interface EventSourceListener {
    fun onEvent(data: String)
    fun onFailure(t: Throwable)
    fun onComplete()
} 