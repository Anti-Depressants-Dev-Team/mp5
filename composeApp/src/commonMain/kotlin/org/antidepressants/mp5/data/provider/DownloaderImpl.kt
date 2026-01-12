package org.antidepressants.mp5.data.provider

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Custom Downloader implementation for NewPipeExtractor.
 * Uses Java's HttpURLConnection for HTTP requests.
 */
class DownloaderImpl private constructor() : Downloader() {
    
    companion object {
        private var instance: DownloaderImpl? = null
        
        @JvmStatic
        fun getInstance(): DownloaderImpl {
            if (instance == null) {
                instance = DownloaderImpl()
            }
            return instance!!
        }
    }
    
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            // Set request method
            connection.requestMethod = request.httpMethod()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            
            // Set default headers
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            
            // Set custom headers from request
            request.headers().forEach { (key, values) ->
                values.forEach { value ->
                    connection.addRequestProperty(key, value)
                }
            }
            
            // Handle request body for POST requests
            val dataToSend = request.dataToSend()
            if (dataToSend != null && dataToSend.isNotEmpty()) {
                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(dataToSend)
                    os.flush()
                }
            }
            
            // Get response
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage
            
            // Read response body
            val responseBody = try {
                if (responseCode >= 400) {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } else {
                    connection.inputStream.bufferedReader().use { it.readText() }
                }
            } catch (e: IOException) {
                ""
            }
            
            // Get response headers
            val responseHeaders = mutableMapOf<String, List<String>>()
            connection.headerFields.forEach { (key, value) ->
                if (key != null) {
                    responseHeaders[key] = value
                }
            }
            
            return Response(
                responseCode,
                responseMessage,
                responseHeaders,
                responseBody,
                request.url()
            )
        } finally {
            connection.disconnect()
        }
    }
}
