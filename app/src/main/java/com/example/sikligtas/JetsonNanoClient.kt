package com.example.sikligtas

import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.Socket

interface OnDataReceivedListener {
    fun onDataReceived(data: String)
}

interface OnConnectionErrorListener {
    fun onConnectionError(exception: Exception)
}

@OptIn(DelicateCoroutinesApi::class)
class JetsonNanoClient(host: String, port: Int) {
    private val processedKeys = mutableSetOf<String>()

    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var isConnected = true
    private var initialDataReceived = false

    private var onDataReceivedListener: OnDataReceivedListener? = null
    private var onConnectionErrorListener: OnConnectionErrorListener? = null

    fun isInitialDataReceived(): Boolean {
        return initialDataReceived
    }

    fun setOnDataReceivedListener(listener: OnDataReceivedListener) {
        onDataReceivedListener = listener
    }

    fun setOnConnectionErrorListener(listener: OnConnectionErrorListener) {
        onConnectionErrorListener = listener
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            while (isConnected) {
                try {
                    socket = Socket(host, port)
                    input = socket!!.getInputStream()
                    output = socket!!.getOutputStream() // For response time

                    val bufferSize = 1024 // Adjust the buffer size based on your requirements
                    val buffer = ByteArray(bufferSize)

                    while (isConnected) {
                        Log.d("JetsonNanoClient", "Successfully Connected")
                        val bytesRead = input!!.read(buffer)
                        if (bytesRead != -1) {
                            val data = String(buffer, 0, bytesRead)
                            val key = generateKey(data) // Generate a key for the data
                            if (!processedKeys.contains(key)) { // Check if the key has already been processed
                                processedKeys.add(key) // Add the key to the set of processed keys
                                withContext(Dispatchers.Main) {
                                    processData(data)
                                }
                            }
                            sendData("OK") // For response time
                        } else {
                            break
                        }
                    }

                    socket!!.close()
                } catch (e: ConnectException) {
                    Log.d("JetsonNanoClient", "Failed to Connect")
                    onConnectionErrorListener?.onConnectionError(e)
                    isConnected = false
                } catch (e: Exception) {
                    Log.e("JetsonNanoClient", "Error: ", e)
                    delay(5000)
                }
            }
        }
    }

    private fun generateKey(data: String): String {
        // Generate a unique key for the data, e.g., using a hash function
        return data.hashCode().toString()
    }

    private fun processData(data: String) {
        if (data.trim() == "success") {
            initialDataReceived = true
        } else {
            // Original data processing logic...
            val outputList: List<String> = data.split(",")
            val type = outputList[0]
            val direction = outputList[1]
            val distance = outputList[2]
            val hazard = outputList[3]
            val id = outputList[4]

            // Call the onDataReceived() function of the listener with the extracted parameters
            onDataReceivedListener?.onDataReceived("$type,$direction,$distance,$hazard,$id")

            Log.d("JetsonNanoClient", "Received data: $data")
        }
    }

    private suspend fun sendData(data: String) {
        withContext(Dispatchers.IO) {
            try {
                output?.write(data.toByteArray())
                output?.flush()
                Log.d("JetsonNanoClient", "Sent data: $data")
            } catch (e: Exception) {
                Log.e("JetsonNanoClient", "Failed to send data", e)
            }
        }
    }

    fun disconnect() {
        isConnected = false
        socket?.close()
        input?.close()
        output?.close()

        onDataReceivedListener = null
    }
}
