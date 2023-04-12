package com.example.sikligtas

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.Socket

interface OnDataReceivedListener {
    fun onDataReceived(data: String)
}

class JetsonNanoClient(host: String, port: Int) {
    private val TAG = "JetsonNanoClient"
    private val processedKeys = mutableSetOf<String>()

    private var socket: Socket? = null
    private var input: InputStream? = null

    private var onDataReceivedListener: OnDataReceivedListener? = null

    fun setOnDataReceivedListener(listener: OnDataReceivedListener) {
        onDataReceivedListener = listener
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                socket = Socket(host, port)
                input = socket!!.getInputStream()

                val bufferSize = 1024 // Adjust the buffer size based on your requirements
                val buffer = ByteArray(bufferSize)

                while (true) {
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
                    } else {
                        break
                    }
                }

                socket!!.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error: ", e)
            }
        }
    }

    private fun generateKey(data: String): String {
        // Generate a unique key for the data, e.g., using a hash function
        return data.hashCode().toString()
    }

    private fun processData(data: String) {
        // Process data received from Jetson Nano
        val outputList: List<String> = data.split(",")
        val type = outputList[0]
        val direction = outputList[1]
        val distance = outputList[2]
        val hazard = outputList[3]

        // Call the onDataReceived() function of the listener with the extracted parameters
        onDataReceivedListener?.onDataReceived("$type,$direction,$distance,$hazard")

        Log.d(TAG, "Received data: $data")
    }

    fun close() {
        socket?.close()
        input?.close()
    }
}