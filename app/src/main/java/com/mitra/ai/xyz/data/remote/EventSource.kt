package com.mitra.ai.xyz.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okio.Buffer
import java.io.IOException

class EventSource private constructor(
    private val client: OkHttpClient,
    private val request: Request,
    private val listener: EventSourceListener
) {
    private var call: Call? = null
    private var isClosed = false

    fun connect() {
        if (isClosed) return
        
        call = client.newCall(request).apply {
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!isClosed) {
                        listener.onFailure(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        if (!isClosed) {
                            listener.onFailure(IOException("Unexpected response: ${response.code}"))
                        }
                        return
                    }

                    response.body?.source()?.let { source ->
                        val buffer = Buffer()
                        var currentLine = StringBuilder()

                        try {
                            while (!isClosed) {
                                // Read a single byte at a time to process immediately
                                val byte = source.read(buffer, 1)
                                if (byte == -1L) break // EOF

                                val b = buffer.readByte()
                                if (b == '\n'.code.toByte()) {
                                    val line = currentLine.toString()
                                    if (line.startsWith("data: ")) {
                                        val data = line.substring(6)
                                        if (data == "[DONE]") {
                                            if (!isClosed) {
                                                listener.onComplete()
                                            }
                                            return
                                        }
                                        if (!isClosed) {
                                            listener.onEvent(data)
                                        }
                                    }
                                    currentLine = StringBuilder()
                                } else {
                                    currentLine.append(b.toInt().toChar())
                                }
                            }
                        } catch (e: IOException) {
                            if (!isClosed) {
                                listener.onFailure(e)
                            }
                        } finally {
                            response.close()
                        }
                    }
                }
            })
        }
    }

    fun close() {
        isClosed = true
        call?.cancel()
    }

    companion object {
        fun create(
            client: OkHttpClient,
            request: Request,
            listener: EventSourceListener
        ): EventSource {
            return EventSource(client, request, listener)
        }

        fun asFlow(
            client: OkHttpClient,
            request: Request
        ): Flow<String> = callbackFlow {
            val listener = object : EventSourceListener {
                override fun onEvent(data: String) {
                    trySend(data)
                }

                override fun onFailure(t: Throwable) {
                    close(t)
                }

                override fun onComplete() {
                    close()
                }
            }

            val eventSource = create(client, request, listener)
            eventSource.connect()

            awaitClose {
                eventSource.close()
            }
        }
    }
}

interface EventSourceListener {
    fun onEvent(data: String)
    fun onFailure(t: Throwable)
    fun onComplete()
} 