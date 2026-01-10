import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.antidepressants.mp5.App
import org.antidepressants.mp5.ui.shell.AppWindow

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "mp5",
        // transparent = true, // Enable for custom glass window
        // undecorated = true // Enable for custom controls
    ) {
        AppWindow()
    }
}
