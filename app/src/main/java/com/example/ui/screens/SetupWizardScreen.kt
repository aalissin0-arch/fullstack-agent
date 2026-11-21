package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AgentStackConfig
import com.example.model.FaceStyle
import com.example.model.IdentityPreset
import com.example.model.MicMode
import com.example.model.PermissionMode
import com.example.model.StackPiece
import com.example.model.VoiceEngine
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun SetupWizardScreen(
    initialConfig: AgentStackConfig,
    canCancel: Boolean,
    onComplete: (AgentStackConfig) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var draft by remember(initialConfig) { mutableStateOf(initialConfig) }

    BackHandler(enabled = step > 0 || canCancel) {
        if (step > 0) {
            step -= 1
        } else if (canCancel) {
            onCancel()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            // Top Wizard Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (step > 0 || canCancel) {
                        IconButton(
                            onClick = {
                                if (step > 0) step -= 1 else onCancel()
                            },
                            modifier = Modifier.testTag("wizard_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous step"
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Fullstack Agent Setup",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Step ${step + 1} of $totalSteps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (canCancel) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("wizard_skip_button")
                    ) {
                        Text("Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (step + 1).toFloat() / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Wizard Body
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                label = "wizard_step"
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentStep) {
                        0 -> WizardHomeStep(
                            draft = draft,
                            onUpdate = { draft = it }
                        )
                        1 -> WizardMenuStep(
                            draft = draft,
                            onUpdate = { draft = it }
                        )
                        2 -> WizardIdentityStep(
                            draft = draft,
                            onUpdate = { draft = it }
                        )
                        3 -> WizardVaultStep(
                            draft = draft,
                            onUpdate = { draft = it }
                        )
                        4 -> WizardVoiceStep(
                            draft = draft,
                            onUpdate = { draft = it }
                        )
                        5 -> WizardFaceAndWireStep(
                            draft = draft,
                            onUpdate = { draft = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("wizard_prev_button")
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < totalSteps - 1) {
                            step += 1
                        } else {
                            onComplete(draft)
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("wizard_next_button")
                ) {
                    Text(if (step < totalSteps - 1) "Continue" else "Wire & Launch Agent")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (step < totalSteps - 1) {
                            Icons.AutoMirrored.Filled.ArrowForward
                        } else {
                            Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardHomeStep(
    draft: AgentStackConfig,
    onUpdate: (AgentStackConfig) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.img_circuit_hero),
                contentDescription = stringResource(id = R.string.cd_hero_circuit),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Phase 0 · Agent Home & Adoption",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assemble your agent in plain English",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Everything your agent remembers and uses lives inside one home folder. Nothing you built before is ever deleted or moved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    OutlinedTextField(
        value = draft.userName,
        onValueChange = { onUpdate(draft.copy(userName = it)) },
        label = { Text("Your name (used in the spoken greeting)") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_user_name_input")
    )

    OutlinedTextField(
        value = draft.agentHomeDir,
        onValueChange = { onUpdate(draft.copy(agentHomeDir = it)) },
        label = { Text("Agent home folder") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_home_dir_input")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUpdate(draft.copy(adoptionMode = !draft.adoptionMode)) }
            .testTag("wizard_adoption_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Adopt an existing CLAUDE.md or setup",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Keep your existing agent identity and vault paths intact instead of starting fresh.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = draft.adoptionMode,
                onCheckedChange = { onUpdate(draft.copy(adoptionMode = it)) }
            )
        }
    }
}

@Composable
private fun WizardMenuStep(
    draft: AgentStackConfig,
    onUpdate: (AgentStackConfig) -> Unit
) {
    Text(
        text = "Phase 1 · Pick your stack pieces",
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = "The core stack includes Memory, Voice, and Face. Hands is an optional spatial board you can include now or add later.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = setOf(
                StackPiece.MEMORY_VAULT,
                StackPiece.VOICE_BACKTALK,
                StackPiece.FACE_VISUALIZER
            ).all { it in draft.enabledPieces },
            onClick = {
                onUpdate(
                    draft.copy(
                        enabledPieces = setOf(
                            StackPiece.MEMORY_VAULT,
                            StackPiece.VOICE_BACKTALK,
                            StackPiece.FACE_VISUALIZER
                        )
                    )
                )
            },
            label = { Text("Recommended Core Stack") },
            modifier = Modifier.testTag("wizard_core_stack_chip")
        )
        FilterChip(
            selected = draft.enabledPieces.size == StackPiece.entries.size,
            onClick = {
                onUpdate(draft.copy(enabledPieces = StackPiece.entries.toSet()))
            },
            label = { Text("All 4 Pieces") },
            modifier = Modifier.testTag("wizard_all_stack_chip")
        )
    }

    StackPiece.entries.forEach { piece ->
        val selected = piece in draft.enabledPieces
        val icon = when (piece) {
            StackPiece.MEMORY_VAULT -> Icons.Default.Memory
            StackPiece.VOICE_BACKTALK -> Icons.Default.RecordVoiceOver
            StackPiece.FACE_VISUALIZER -> Icons.Default.AutoAwesome
            StackPiece.HANDS_BOARD -> Icons.Default.PanTool
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val next = if (selected) {
                        draft.enabledPieces - piece
                    } else {
                        draft.enabledPieces + piece
                    }
                    onUpdate(draft.copy(enabledPieces = next))
                }
                .testTag("wizard_piece_${piece.name.lowercase()}"),
            border = if (selected) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = piece.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (piece.optionalAddon) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "OPTIONAL",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = piece.plainSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = selected,
                    onCheckedChange = { checked ->
                        val next = if (checked) {
                            draft.enabledPieces + piece
                        } else {
                            draft.enabledPieces - piece
                        }
                        onUpdate(draft.copy(enabledPieces = next))
                    }
                )
            }
        }
    }
}

@Composable
private fun WizardIdentityStep(
    draft: AgentStackConfig,
    onUpdate: (AgentStackConfig) -> Unit
) {
    Text(
        text = "Phase 2 · Agent Identity (Three Doors)",
        style = MaterialTheme.typography.headlineSmall
    )
    if (draft.adoptionMode) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = "Adoption mode is enabled — keeping ${draft.agentName}'s existing personality and identity.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    IdentityPreset.entries.forEach { preset ->
        val selected = draft.identityPreset == preset
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val nextName = when (preset) {
                        IdentityPreset.DEFAULT_ASSISTANT -> "Atlas"
                        IdentityPreset.CUSTOM_NAMED -> if (draft.agentName == "Atlas") "Nova" else draft.agentName
                        IdentityPreset.FROM_SCRATCH -> draft.agentName
                    }
                    onUpdate(draft.copy(identityPreset = preset, agentName = nextName))
                }
                .testTag("wizard_identity_${preset.name.lowercase()}"),
            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = preset.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = preset.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    OutlinedTextField(
        value = draft.agentName,
        onValueChange = { onUpdate(draft.copy(agentName = it)) },
        label = { Text("Agent name (printed on the circuit chip)") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_agent_name_input")
    )

    if (draft.identityPreset == IdentityPreset.FROM_SCRATCH) {
        OutlinedTextField(
            value = draft.agentRole,
            onValueChange = { onUpdate(draft.copy(agentRole = it)) },
            label = { Text("Agent role") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.agentPersonality,
            onValueChange = { onUpdate(draft.copy(agentPersonality = it)) },
            label = { Text("Personality & tone") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WizardVaultStep(
    draft: AgentStackConfig,
    onUpdate: (AgentStackConfig) -> Unit
) {
    Text(
        text = "Phase 2 · Memory Vault Setup",
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = "Your vault holds plain-text Markdown files so your agent keeps context across sessions. Vaults are pointed at in place and never moved.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = !draft.usingExistingVault,
            onClick = {
                onUpdate(draft.copy(usingExistingVault = false, vaultPath = "~/AgentVault"))
            },
            label = { Text("Create Fresh Vault") },
            modifier = Modifier.testTag("wizard_fresh_vault_chip")
        )
        FilterChip(
            selected = draft.usingExistingVault,
            onClick = {
                onUpdate(draft.copy(usingExistingVault = true, vaultPath = "~/Documents/NotesVault"))
            },
            label = { Text("Adopt Existing Vault") },
            modifier = Modifier.testTag("wizard_existing_vault_chip")
        )
    }

    OutlinedTextField(
        value = draft.vaultPath,
        onValueChange = { onUpdate(draft.copy(vaultPath = it)) },
        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
        label = { Text("Vault directory path") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_vault_path_input")
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Register in Obsidian config", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Adds this vault to obsidian.json so Obsidian opens straight into it on launch.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = draft.obsidianRegistered,
                onCheckedChange = { onUpdate(draft.copy(obsidianRegistered = it)) }
            )
        }
    }

    if (!draft.usingExistingVault) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        ) {
            Text(
                text = "Backup note: A fresh vault lives locally on this device. Use Git sync or periodic exports if you want off-device backups.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun WizardVoiceStep(
    draft: AgentStackConfig,
    onUpdate: (AgentStackConfig) -> Unit
) {
    Text(
        text = "Phase 2 · Voice, Mic & Permissions",
        style = MaterialTheme.typography.headlineSmall
    )

    Text("Microphone Mode", style = MaterialTheme.typography.titleMedium)
    MicMode.entries.forEach { mode ->
        val selected = draft.micMode == mode
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdate(draft.copy(micMode = mode)) }
                .testTag("wizard_mic_${mode.configValue}"),
            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(mode.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        mode.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Text("Voice Engine", style = MaterialTheme.typography.titleMedium)
    VoiceEngine.entries.forEach { engine ->
        val selected = draft.voiceEngine == engine
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdate(draft.copy(voiceEngine = engine)) }
                .testTag("wizard_voice_${engine.configValue}"),
            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(engine.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        engine.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Text("Action Permissions", style = MaterialTheme.typography.titleMedium)
    PermissionMode.entries.forEach { perm ->
        val selected = draft.permissionMode == perm
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdate(draft.copy(permissionMode = perm)) }
                .testTag("wizard_perm_${perm.configValue}"),
            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(perm.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        perm.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardFaceAndWireStep(
    draft: AgentStackConfig,
    onUpdate: (AgentStackConfig) -> Unit
) {
    Text(
        text = "Phase 3–4 · Default Face & Seam Wiring",
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = "The voice writes short status notes (.voice_state) and the face reads them — that shared bus is the whole seam.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FaceStyle.entries.forEach { face ->
            val selected = draft.defaultFace == face
            FilterChip(
                selected = selected,
                onClick = { onUpdate(draft.copy(defaultFace = face)) },
                label = { Text(face.label) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("wizard_face_${face.id}")
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUpdate(draft.copy(mechanicRuleEnabled = !draft.mechanicRuleEnabled)) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Make the agent the mechanic", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Appends the self-diagnosis instruction to CLAUDE.md so your agent fixes broken seams itself.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = draft.mechanicRuleEnabled,
                onCheckedChange = { onUpdate(draft.copy(mechanicRuleEnabled = it)) }
            )
        }
    }

    // Config Seam Summary Card
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Wiring Summary",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "Greeting: \"${draft.greetingLine}\"",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily
            )
            Text(
                text = "Pieces: ${draft.enabledPieces.joinToString { it.repoName }}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily
            )
            Text(
                text = "Face: ${draft.defaultFace.id} · Mic: ${draft.micMode.configValue} · Perms: ${draft.permissionMode.configValue}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily
            )
        }
    }
}
