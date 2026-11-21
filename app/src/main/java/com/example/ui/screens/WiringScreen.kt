package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.MicMode
import com.example.model.PermissionMode
import com.example.model.StackPiece
import com.example.model.VoiceEngine
import com.example.ui.AgentUiState
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun WiringScreen(
    state: AgentUiState,
    backtalkJson: String,
    visualizerJson: String,
    barehandsJson: String,
    onToggleMicMode: () -> Unit,
    onTogglePermissionMode: () -> Unit,
    onToggleVoiceEngine: () -> Unit,
    onSimulateBreak: () -> Unit,
    onRepairWiring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = state.config
    val healthy = cfg.wiringHealthy

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Bus Telemetry Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                1.dp,
                if (healthy) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.error
            )
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (healthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (healthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (healthy) "Seam Bus Wired & Active" else "Bus Path Mismatch Detected",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (healthy) state.busState.stateToken.uppercase() else "STALE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = if (healthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Voice writes .voice_state and .voice_waveform; the face visualizer reads them continuously.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = ".voice_waveform = ${"%.2f".format(state.waveformEnergy)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily
                )
                LinearProgressIndicator(
                    progress = { state.waveformEnergy.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (healthy) {
                        OutlinedButton(
                            onClick = onSimulateBreak,
                            modifier = Modifier.testTag("simulate_bus_break_button")
                        ) {
                            Text("Test Seam Break")
                        }
                    } else {
                        Button(
                            onClick = onRepairWiring,
                            modifier = Modifier.testTag("repair_bus_seam_button")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-Repair Seam")
                        }
                    }
                }
            }
        }

        // Quick Config Knobs (Phase 4)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cable, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live Runtime Knobs", style = MaterialTheme.typography.titleMedium)
                }

                SettingToggleRow(
                    title = "Hands-Free Microphone",
                    subtitle = if (cfg.micMode == MicMode.HANDS_FREE) {
                        "mic_mode = \"open\" (always listening)"
                    } else {
                        "mic_mode = \"ptt\" (push to talk)"
                    },
                    checked = cfg.micMode == MicMode.HANDS_FREE,
                    onCheckedChange = { onToggleMicMode() },
                    testTag = "wiring_mic_mode_switch"
                )

                SettingToggleRow(
                    title = "Auto-Approve Tool Actions",
                    subtitle = if (cfg.permissionMode == PermissionMode.BYPASS_PERMISSIONS) {
                        "permission_mode = \"bypassPermissions\""
                    } else {
                        "permission_mode = \"ask\""
                    },
                    checked = cfg.permissionMode == PermissionMode.BYPASS_PERMISSIONS,
                    onCheckedChange = { onTogglePermissionMode() },
                    testTag = "wiring_permission_switch"
                )

                SettingToggleRow(
                    title = "ElevenLabs Neural Voice",
                    subtitle = if (cfg.voiceEngine == VoiceEngine.ELEVEN_LABS) {
                        "voice_engine = \"elevenlabs\""
                    } else {
                        "voice_engine = \"builtin\" (local offline)"
                    },
                    checked = cfg.voiceEngine == VoiceEngine.ELEVEN_LABS,
                    onCheckedChange = { onToggleVoiceEngine() },
                    testTag = "wiring_voice_engine_switch"
                )
            }
        }

        // Generated Config Files Inspector
        ConfigCodeBlock(
            filePath = "backtalk/backtalk.json",
            jsonText = backtalkJson
        )
        ConfigCodeBlock(
            filePath = "ai-visualizer/ai-visualizer.json",
            jsonText = visualizerJson
        )
        if (StackPiece.HANDS_BOARD in cfg.enabledPieces) {
            ConfigCodeBlock(
                filePath = "barehands/barehands.json",
                jsonText = barehandsJson
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun ConfigCodeBlock(
    filePath: String,
    jsonText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = filePath,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFamily,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = jsonText,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily
            )
        }
    }
}
