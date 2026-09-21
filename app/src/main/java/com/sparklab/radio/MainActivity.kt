package com.sparklab.radio

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sparklab.radio.data.playback.MediaControllerPlaybackRepository
import com.sparklab.radio.ui.navigation.RadioAppScaffold
import com.sparklab.radio.ui.theme.RadioSparkTheme
import com.sparklab.radio.ui.viewmodel.RadioViewModel

/**
 * Single-Activity, Compose host.
 *
 *  • connects the MediaController bridge so the UI can drive playback,
 *  • requests notification permission (Android 13+) for the media notification,
 *  • applies the user's light/dark preference from Settings.
 */
class MainActivity : ComponentActivity() {

    private val vm: RadioViewModel by viewModels()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bridge to the playback service (MediaSession).
        (getApplication() as RadioApp).container.playbackRepository.let { repo ->
            if (repo is MediaControllerPlaybackRepository) repo.connect()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by vm.settingsRepo.settings.collectAsState(
                initial = com.sparklab.radio.domain.model.UserSettings(),
            )
            RadioSparkTheme(darkTheme = settings.darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RadioAppScaffold(vm)
                }
            }
        }
    }

    override fun onDestroy() {
        val repo = (application as RadioApp).container.playbackRepository
        if (repo is MediaControllerPlaybackRepository && isFinishing) repo.release()
        super.onDestroy()
    }
}
