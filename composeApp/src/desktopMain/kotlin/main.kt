import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.antidepressants.mp5.App
import org.antidepressants.mp5.player.DemoPlayer
import org.antidepressants.mp5.player.DesktopAudioPlayer

fun main() = application {
    // Initialize VLCJ audio player for desktop
    val audioPlayer = try {
        DesktopAudioPlayer()
    } catch (e: Exception) {
        println("[main] Failed to create audio player: ${e.message}")
        null
    }
    
    // Wire audio player to the demo controller
    audioPlayer?.let { player ->
        DemoPlayer.controller.setAudioPlayer(player)
        // Load the demo track with the player
        DemoPlayer.controller.loadDemoTrack()
    }
    
    Window(
        onCloseRequest = {
            // Cleanup on close
            audioPlayer?.release()
            exitApplication()
        },
        title = "Psychopath Player",
        // transparent = true, // Enable for custom glass window
        // undecorated = true // Enable for custom controls
    ) {
        // Use App() which wraps AppWindow with Mp5Theme
        App()
    }
}
