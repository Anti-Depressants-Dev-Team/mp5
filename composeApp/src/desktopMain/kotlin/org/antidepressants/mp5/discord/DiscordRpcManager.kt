package org.antidepressants.mp5.discord

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antidepressants.mp5.domain.model.Track
import org.antidepressants.mp5.player.PlaybackState
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Discord Rich Presence manager for Windows Desktop.
 * Uses Discord's local IPC socket to communicate presence updates.
 */
class DiscordRpcManager {
    
    companion object {
        private const val CLIENT_ID = "1461757069769314425"
        private val json = Json { 
            ignoreUnknownKeys = true 
            encodeDefaults = false
        }
    }
    
    private var pipe: RandomAccessFile? = null
    @Volatile private var connected = false
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lock = Any()
    private var lastConnectAttempt = 0L
    private val reconnectCooldownMs = 5000L // Don't spam reconnect attempts
    
    fun connect(): Boolean {
        synchronized(lock) {
            if (connected && pipe != null) return true
            
            // Cooldown to prevent spamming connection attempts
            val now = System.currentTimeMillis()
            if (now - lastConnectAttempt < reconnectCooldownMs) {
                return false
            }
            lastConnectAttempt = now
            
            try {
                // Close any stale pipe
                try { pipe?.close() } catch (_: Exception) {}
                pipe = null
                connected = false
                
                for (i in 0..9) {
                    try {
                        val pipePath = "\\\\.\\pipe\\discord-ipc-$i"
                        val testPipe = RandomAccessFile(pipePath, "rw")
                        pipe = testPipe
                        connected = true
                        println("[DiscordRPC] Connected to pipe $i")
                        
                        if (sendHandshake()) {
                            return true
                        } else {
                            // Handshake failed, try next pipe
                            try { testPipe.close() } catch (_: Exception) {}
                            pipe = null
                            connected = false
                        }
                    } catch (_: Exception) { }
                }
                println("[DiscordRPC] Could not connect to Discord (is Discord running?)")
                return false
            } catch (e: Exception) {
                println("[DiscordRPC] Connection error: ${e.message}")
                connected = false
                return false
            }
        }
    }
    
    fun updatePresence(track: Track?, playbackState: PlaybackState, positionMs: Long = 0, durationMs: Long = 0) {
        scope.launch {
            try {
                if (!connected && !connect()) return@launch
                
                val activity = if (track != null && playbackState == PlaybackState.PLAYING) {
                    val startTime = System.currentTimeMillis() - positionMs
                    val endTime = if (durationMs > 0) startTime + durationMs else null
                    
                    Activity(
                        details = track.title.take(128),
                        state = "by ${track.artist}".take(128),
                        timestamps = ActivityTimestamps(
                            start = startTime,
                            end = endTime  // This enables the progress bar!
                        ),
                        assets = ActivityAssets(
                            largeImage = track.thumbnailUrl ?: "psychopath_icon",  // Use thumbnail or app icon
                            largeText = track.album ?: track.title,
                            smallImage = "play_icon",  // Small play indicator
                            smallText = "Playing"
                        ),
                        type = 2  // 2 = Listening activity (shows "Listening to")
                    )
                } else if (track != null && playbackState == PlaybackState.PAUSED) {
                    Activity(
                        details = track.title.take(128),
                        state = "by ${track.artist} (Paused)".take(128),
                        assets = ActivityAssets(
                            largeImage = track.thumbnailUrl ?: "psychopath_icon",
                            largeText = track.album ?: track.title,
                            smallImage = "pause_icon",
                            smallText = "Paused"
                        ),
                        type = 2
                    )
                } else null
                
                if (!sendActivity(activity)) {
                    // Failed to send, mark as disconnected for retry
                    synchronized(lock) { connected = false }
                }
            } catch (e: Exception) {
                println("[DiscordRPC] Update error: ${e.message}")
                synchronized(lock) { connected = false }
            }
        }
    }
    
    fun clearPresence() {
        scope.launch {
            try {
                if (connected) sendActivity(null)
            } catch (_: Exception) { }
        }
    }
    
    fun disconnect() {
        synchronized(lock) {
            try { pipe?.close() } catch (_: Exception) { }
            pipe = null
            connected = false
        }
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
    
    private fun sendHandshake(): Boolean {
        return try {
            val handshake = json.encodeToString(Handshake(v = 1, client_id = CLIENT_ID))
            if (!sendPacket(0, handshake)) return false
            
            // Read and validate the response
            val response = readResponse()
            println("[DiscordRPC] Handshake response: ${response.take(200)}")
            response.contains("DISPATCH") || response.contains("READY")
        } catch (e: Exception) {
            println("[DiscordRPC] Handshake error: ${e.message}")
            false
        }
    }
    
    private fun readResponse(): String {
        val currentPipe = pipe ?: throw Exception("Pipe is null")
        val header = ByteArray(8)
        currentPipe.readFully(header)
        
        // Debug raw header
        val hexHeader = header.joinToString(" ") { "%02X".format(it) }
        println("[DiscordRPC] Read Header: $hexHeader")
        
        val headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val opcode = headerBuffer.getInt()
        val length = headerBuffer.getInt()
        
        println("[DiscordRPC] Opcode: $opcode, Length: $length")
        
        if (length > 65536) throw Exception("Response too large: $length")
        
        val data = ByteArray(length)
        currentPipe.readFully(data)
        return String(data, Charsets.UTF_8)
    }
    
    private fun sendActivity(activity: Activity?): Boolean {
        return try {
            val payload = RpcPayload(
                cmd = "SET_ACTIVITY",
                args = ActivityArgs(pid = ProcessHandle.current().pid().toInt(), activity = activity),
                nonce = System.nanoTime().toString()
            )
            val payloadJson = json.encodeToString(payload)
            println("[DiscordRPC] Sending activity: ${payloadJson.take(300)}")
            val result = sendPacket(1, payloadJson)
            if (result) {
                // Try to read response
                try {
                    val response = readResponse()
                    println("[DiscordRPC] Activity response: ${response.take(200)}")
                } catch (e: Exception) {
                    println("[DiscordRPC] Could not read response (Disconnecting): ${e.message}")
                    // CRITICAL: Disconnect on read error to prevent desync
                    synchronized(lock) { 
                        connected = false 
                        try { pipe?.close() } catch (_: Exception) {}
                        pipe = null
                    }
                }
            }
            result
        } catch (e: Exception) {
            println("[DiscordRPC] sendActivity error: ${e.message}")
            false
        }
    }
    
    private fun sendPacket(opcode: Int, data: String): Boolean {
        synchronized(lock) {
            val currentPipe = pipe ?: return false
            if (!connected) return false
            
            return try {
                val dataBytes = data.toByteArray(Charsets.UTF_8)
                val buffer = ByteBuffer.allocate(8 + dataBytes.size)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                buffer.putInt(opcode)
                buffer.putInt(dataBytes.size)
                buffer.put(dataBytes)
                currentPipe.write(buffer.array())
                true
            } catch (e: Exception) {
                println("[DiscordRPC] sendPacket error: ${e.message}")
                connected = false
                try { currentPipe.close() } catch (_: Exception) {}
                pipe = null
                false
            }
        }
    }
}

@Serializable data class RpcPayload(val cmd: String, val args: ActivityArgs, val nonce: String)
@Serializable data class ActivityArgs(val pid: Int, val activity: Activity?)
@Serializable data class Activity(val details: String? = null, val state: String? = null, val timestamps: ActivityTimestamps? = null, val assets: ActivityAssets? = null, val type: Int = 0)
@Serializable data class ActivityTimestamps(val start: Long? = null, val end: Long? = null)
@Serializable data class ActivityAssets(val largeImage: String? = null, val largeText: String? = null, val smallImage: String? = null, val smallText: String? = null)
@Serializable data class Handshake(val v: Int, val client_id: String)

object GlobalDiscordRpc { val manager = DiscordRpcManager() }

