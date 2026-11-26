package app.lody.android.diagnostics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.lody.android.chat.ImageDebugScreen
import app.lody.android.designsystem.LodyTheme

/**
 * Offline fixture for the transcript image controls: loaded, loading and failed user tiles and
 * agent cards, plus the image stack. No login, runtime or network.
 */
class ImageStatesProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LodyTheme { ImageDebugScreen(onBack = { finish() }) } }
    }
}
