package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.BoardCard
import com.example.model.FaceStyle
import com.example.model.LaunchMode
import com.example.model.MicMode
import com.example.model.StackPiece
import com.example.model.VoiceBusState
import com.example.ui.AgentUiState
import com.example.ui.components.AgentFaceVisualizer
import com.example.ui.theme.CircuitAmber
import com.example.ui.theme.CircuitCyan
import com.example.ui.theme.CircuitEmerald
import kotlin.math.roundToInt

@Composable
fun StageScreen(
    state: AgentUiState,
    onLaunchModeSelected: (LaunchMode) -> Unit,
    onFaceSelected: (FaceStyle) -> Unit,
    onPlayGreeting: () -> Unit,
    onStartPtt: (Boolean) -> Unit,
    onStopPtt: (String?) -> Unit,
    onToggleMicMode: () -> Unit,
    onSubmitPrompt: (String) -> Unit,
    onPresentCard: (String, String) -> Unit,
    onMoveCard: (String, Float, Float) -> Unit,
    onPersistCards: () -> Unit,
    onRemoveCard: (String) -> Unit,
    onOpenWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    var promptDraft by remember { mutableStateOf("") }
    var showPresentDialog by remember { mutableStateOf(false) }
    var newCardTitle by remember { mutableStateOf("") }
    var newCardBody by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val showFace = state.launchMode != LaunchMode.CHAT &&
        state.launchMode != LaunchMode.VOICE_AND_HANDS &&
        StackPiece.FACE_VISUALIZER in state.config.enabledPieces

    val showHandsBoard = (state.launchMode == LaunchMode.ALL || state.launchMode == LaunchMode.VOICE_AND_HANDS) &&
        StackPiece.HANDS_BOARD in state.config.enabledPieces

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Launch Mode Switcher (start.sh modes: All / Voice / Hands / Chat)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaunchMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.launchMode == mode,
                    onClick = { onLaunchModeSelected(mode) },
                    label = { Text(mode.label) },
                    modifier = Modifier.testTag("launch_mode_${mode.cliArg}")
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedButton(
                onClick = onOpenWizard,
                modifier = Modifier.testTag("stage_reconfigure_button")
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Setup")
            }
        }

        // Face Visualizer Stage
        AnimatedVisibility(visible = showFace) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AgentFaceVisualizer(
                    faceStyle = state.activeFace,
                    agentName = state.config.agentName,
                    busState = state.busState,
                    waveformEnergy = state.waveformEnergy,
                    waveformBars = state.waveformBars,
                    wiringHealthy = state.config.wiringHealthy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .testTag("agent_face_visualizer")
                )

                // Face Picker + Speak Greeting Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FaceStyle.entries.forEach { face ->
                        FilterChip(
                            selected = state.activeFace == face,
                            onClick = { onFaceSelected(face) },
                            label = { Text(face.label) },
                            modifier = Modifier.testTag("face_chip_${face.id}")
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onPlayGreeting,
                        modifier = Modifier.testTag("play_greeting_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Greet")
                    }
                }
            }
        }

        // Spatial Hands Board (barehands)
        AnimatedVisibility(visible = showHandsBoard) {
            SpatialHandsBoard(
                cards = state.boardCards,
                onMoveCard = onMoveCard,
                onPersistCards = onPersistCards,
                onRemoveCard = onRemoveCard,
                onAddCardClick = { showPresentDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            )
        }

        // Prompt notice if user selected Voice+Hands mode without Hands enabled
        if (state.launchMode == LaunchMode.VOICE_AND_HANDS && StackPiece.HANDS_BOARD !in state.config.enabledPieces) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "The Hands board is an optional add-on. Enable it in Setup to pin spatial cards.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenWizard) {
                        Text("Add Hands")
                    }
                }
            }
        }

        // Session Transcript
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (state.messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hold Talk or send a message to wake ${state.config.agentName}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isFromUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (msg.isFromUser) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = msg.senderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = msg.timestampLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Voice Control + Text Input Dock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (StackPiece.VOICE_BACKTALK in state.config.enabledPieces) {
                val isListening = state.busState == VoiceBusState.LISTENING
                val isHandsFree = state.config.micMode == MicMode.HANDS_FREE

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("voice_ptt_button")
                        .pointerInput(isHandsFree, hasMicPermission) {
                            detectTapGestures(
                                onPress = {
                                    if (!hasMicPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                    if (isHandsFree) {
                                        onToggleMicMode()
                                    } else {
                                        onStartPtt(hasMicPermission)
                                        tryAwaitRelease()
                                        onStopPtt(
                                            if (!hasMicPermission) "Status check" else null
                                        )
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isListening -> Icons.Default.GraphicEq
                                isHandsFree -> Icons.Default.Mic
                                else -> Icons.Default.MicOff
                            },
                            contentDescription = "Voice talk control",
                            tint = if (isListening) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isListening -> "Listening"
                                isHandsFree -> "Open Mic"
                                else -> "Hold Talk"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isListening) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = promptDraft,
                onValueChange = { promptDraft = it },
                placeholder = { Text("Ask ${state.config.agentName} or type 'present ...'") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stage_chat_input")
            )

            Button(
                onClick = {
                    val toSend = promptDraft
                    promptDraft = ""
                    onSubmitPrompt(toSend)
                },
                enabled = promptDraft.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("stage_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
            }
        }
    }

    if (showPresentDialog) {
        AlertDialog(
            onDismissRequest = { showPresentDialog = false },
            title = { Text("Present Card to Hands Board") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCardTitle,
                        onValueChange = { newCardTitle = it },
                        label = { Text("Card title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCardBody,
                        onValueChange = { newCardBody = it },
                        label = { Text("Content") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPresentCard(newCardTitle, newCardBody)
                        newCardTitle = ""
                        newCardBody = ""
                        showPresentDialog = false
                    }
                ) {
                    Text("Present")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SpatialHandsBoard(
    cards: List<BoardCard>,
    onMoveCard: (String, Float, Float) -> Unit,
    onPersistCards: () -> Unit,
    onRemoveCard: (String) -> Unit,
    onAddCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boardWidthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
            val boardHeightPx = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)

            // Header overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BAREHANDS SPATIAL BOARD · Drag cards",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag("present_card_button")
                ) {
                    IconButton(
                        onClick = onAddCardClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Present new card",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            cards.forEach { card ->
                val accent = when (card.accentIndex % 3) {
                    0 -> CircuitEmerald
                    1 -> CircuitCyan
                    else -> CircuitAmber
                }
                val xPx = (card.xFraction * boardWidthPx).roundToInt()
                val yPx = (card.yFraction * boardHeightPx).roundToInt()

                Card(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .width(168.dp)
                        .pointerInput(card.id, boardWidthPx, boardHeightPx) {
                            detectDragGestures(
                                onDragEnd = { onPersistCards() }
                            ) { change, dragAmount ->
                                change.consume()
                                val nextX = card.xFraction + (dragAmount.x / boardWidthPx)
                                val nextY = card.yFraction + (dragAmount.y / boardHeightPx)
                                onMoveCard(card.id, nextX, nextY)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    ),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(3.dp)
                                    .background(accent, RoundedCornerShape(2.dp))
                            )
                            IconButton(
                                onClick = { onRemoveCard(card.id) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove card",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = card.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
