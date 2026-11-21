package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AgentRepository
import com.example.model.AgentStackConfig
import com.example.model.BoardCard
import com.example.model.ChatMessage
import com.example.model.FaceStyle
import com.example.model.IdentityPreset
import com.example.model.LaunchMode
import com.example.model.MicMode
import com.example.model.PermissionMode
import com.example.model.StackPiece
import com.example.model.StackRepoStatus
import com.example.model.TroubleshootingItem
import com.example.model.VaultNote
import com.example.model.VoiceBusState
import com.example.model.VoiceEngine
import com.example.model.WiringDiagnostic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.sin
import kotlin.random.Random

data class AgentUiState(
    val isLoading: Boolean = true,
    val config: AgentStackConfig = AgentStackConfig(),
    val vaultNotes: List<VaultNote> = emptyList(),
    val boardCards: List<BoardCard> = emptyList(),
    val launchMode: LaunchMode = LaunchMode.ALL,
    val activeFace: FaceStyle = FaceStyle.BOARD,
    val busState: VoiceBusState = VoiceBusState.IDLE,
    val waveformEnergy: Float = 0.08f,
    val waveformBars: List<Float> = List(24) { 0.1f },
    val messages: List<ChatMessage> = emptyList(),
    val repoStatuses: List<StackRepoStatus> = emptyList(),
    val troubleshootingItems: List<TroubleshootingItem> = emptyList(),
    val diagnostics: List<WiringDiagnostic> = emptyList(),
    val ttsReady: Boolean = false,
    val lastStatusBanner: String? = null,
    val customClaudeMdOverride: String? = null,
    val updateBanner: String? = null
)

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AgentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var busAnimationJob: Job? = null
    private var speakingTimeoutJob: Job? = null
    private var hasPlayedInitialGreeting = false

    init {
        initTextToSpeech()
        observeRepository()
        startBusTelemetryLoop()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            combine(
                repository.configFlow,
                repository.vaultNotesFlow,
                repository.boardCardsFlow
            ) { config, notes, cards ->
                Triple(config, notes, cards)
            }.collect { (config, notes, cards) ->
                val diagnostics = repository.runWiringDiagnostics(config)
                val currentRepos = _uiState.value.repoStatuses.ifEmpty {
                    repository.initialRepoStatuses(config)
                }.map { repo ->
                    val enabled = when (repo.repoName) {
                        "ai-memory-vault" -> StackPiece.MEMORY_VAULT in config.enabledPieces
                        "backtalk" -> StackPiece.VOICE_BACKTALK in config.enabledPieces
                        "ai-visualizer" -> StackPiece.FACE_VISUALIZER in config.enabledPieces
                        "barehands" -> StackPiece.HANDS_BOARD in config.enabledPieces
                        else -> true
                    }
                    repo.copy(installed = enabled)
                }

                _uiState.update { state ->
                    val initialMessages = if (state.messages.isEmpty() && config.setupCompleted) {
                        listOf(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                isFromUser = false,
                                senderName = config.agentName,
                                text = config.greetingLine,
                                timestampLabel = currentTimeLabel()
                            )
                        )
                    } else {
                        state.messages
                    }

                    state.copy(
                        isLoading = false,
                        config = config,
                        vaultNotes = notes,
                        boardCards = cards,
                        activeFace = if (state.isLoading) config.defaultFace else state.activeFace,
                        repoStatuses = currentRepos,
                        troubleshootingItems = repository.troubleshootingCatalog(),
                        diagnostics = diagnostics,
                        messages = initialMessages
                    )
                }
            }
        }
    }

    private fun initTextToSpeech() {
        val context = getApplication<Application>().applicationContext
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        setBusState(VoiceBusState.SPEAKING)
                    }

                    override fun onDone(utteranceId: String?) {
                        setBusState(VoiceBusState.IDLE)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        setBusState(VoiceBusState.IDLE)
                    }
                })
                _uiState.update { it.copy(ttsReady = true) }
            }
        }
    }

    private fun startBusTelemetryLoop() {
        busAnimationJob?.cancel()
        busAnimationJob = viewModelScope.launch {
            var phase = 0f
            while (true) {
                val state = _uiState.value
                val wiringOk = state.config.wiringHealthy
                val effectiveState = if (wiringOk) state.busState else VoiceBusState.IDLE
                phase += when (effectiveState) {
                    VoiceBusState.IDLE -> 0.08f
                    VoiceBusState.LISTENING -> 0.24f
                    VoiceBusState.THINKING -> 0.36f
                    VoiceBusState.SPEAKING -> 0.52f
                }

                val bars = List(24) { idx ->
                    val wave = (sin(phase + idx * 0.45f) + 1f) * 0.5f
                    when (effectiveState) {
                        VoiceBusState.IDLE -> 0.08f + wave * 0.12f
                        VoiceBusState.LISTENING -> 0.22f + wave * 0.48f
                        VoiceBusState.THINKING -> 0.30f + wave * 0.42f
                        VoiceBusState.SPEAKING -> 0.25f + wave * 0.72f + Random.nextFloat() * 0.15f
                    }.coerceIn(0.05f, 1f)
                }
                val avgEnergy = bars.average().toFloat()
                repository.writeVoiceBusFiles(effectiveState.stateToken, avgEnergy)

                _uiState.update {
                    it.copy(
                        waveformEnergy = avgEnergy,
                        waveformBars = bars
                    )
                }
                delay(70L)
            }
        }
    }

    fun setBusState(newState: VoiceBusState) {
        _uiState.update { it.copy(busState = newState) }
    }

    fun setLaunchMode(mode: LaunchMode) {
        _uiState.update {
            it.copy(
                launchMode = mode,
                lastStatusBanner = "Switched launch mode to ${mode.label}"
            )
        }
    }

    fun selectFace(face: FaceStyle) {
        _uiState.update { it.copy(activeFace = face) }
        viewModelScope.launch {
            val updated = _uiState.value.config.copy(defaultFace = face)
            repository.saveConfig(updated)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val updated = _uiState.value.config.copy(darkTheme = !_uiState.value.config.darkTheme)
            repository.saveConfig(updated)
        }
    }

    fun completeWizard(newConfig: AgentStackConfig) {
        viewModelScope.launch {
            val finalized = newConfig.copy(
                setupCompleted = true,
                wiringHealthy = true,
                busDir = "${newConfig.agentHomeDir}/backtalk",
                barehandsStateDir = "${newConfig.agentHomeDir}/barehands/state"
            )
            repository.saveConfig(finalized)
            _uiState.update {
                it.copy(
                    activeFace = finalized.defaultFace,
                    messages = listOf(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            isFromUser = false,
                            senderName = finalized.agentName,
                            text = finalized.greetingLine,
                            timestampLabel = currentTimeLabel()
                        )
                    ),
                    lastStatusBanner = "${finalized.agentName} is wired and online."
                )
            }
            if (StackPiece.VOICE_BACKTALK in finalized.enabledPieces) {
                speakText(finalized.greetingLine)
            }
        }
    }

    fun playLaunchGreeting() {
        val cfg = _uiState.value.config
        if (StackPiece.VOICE_BACKTALK !in cfg.enabledPieces) {
            appendAgentMessage(cfg.greetingLine)
            _uiState.update {
                it.copy(lastStatusBanner = "Voice module disabled — delivered greeting in text.")
            }
            return
        }
        if (!hasPlayedInitialGreeting) {
            hasPlayedInitialGreeting = true
        }
        speakText(cfg.greetingLine)
    }

    fun toggleMicMode() {
        viewModelScope.launch {
            val current = _uiState.value.config
            val nextMode = if (current.micMode == MicMode.PUSH_TO_TALK) {
                MicMode.HANDS_FREE
            } else {
                MicMode.PUSH_TO_TALK
            }
            repository.saveConfig(current.copy(micMode = nextMode))
            val notice = "Mic switched to ${nextMode.title}."
            _uiState.update { it.copy(lastStatusBanner = notice) }
            if (nextMode == MicMode.HANDS_FREE) {
                setBusState(VoiceBusState.LISTENING)
            } else {
                setBusState(VoiceBusState.IDLE)
            }
        }
    }

    fun togglePermissionMode() {
        viewModelScope.launch {
            val current = _uiState.value.config
            val nextPerm = if (current.permissionMode == PermissionMode.ASK) {
                PermissionMode.BYPASS_PERMISSIONS
            } else {
                PermissionMode.ASK
            }
            repository.saveConfig(current.copy(permissionMode = nextPerm))
            _uiState.update { it.copy(lastStatusBanner = "Permissions: ${nextPerm.title}") }
        }
    }

    fun toggleVoiceEngine() {
        viewModelScope.launch {
            val current = _uiState.value.config
            val nextEngine = if (current.voiceEngine == VoiceEngine.LOCAL_BUILTIN) {
                VoiceEngine.ELEVEN_LABS
            } else {
                VoiceEngine.LOCAL_BUILTIN
            }
            repository.saveConfig(current.copy(voiceEngine = nextEngine))
            _uiState.update { it.copy(lastStatusBanner = "Voice engine: ${nextEngine.title}") }
        }
    }

    fun startPushToTalk(hasAudioPermission: Boolean) {
        setBusState(VoiceBusState.LISTENING)
        if (!hasAudioPermission) return

        val context = getApplication<Application>().applicationContext
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            setBusState(VoiceBusState.THINKING)
                        }

                        override fun onError(error: Int) {
                            setBusState(VoiceBusState.IDLE)
                        }

                        override fun onResults(results: Bundle?) {
                            val spoken = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                            if (!spoken.isNullOrBlank()) {
                                submitUserPrompt(spoken)
                            } else {
                                setBusState(VoiceBusState.IDLE)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechRecognizer?.startListening(intent)
            }
        }
    }

    fun stopPushToTalk(fallbackQuickPrompt: String? = null) {
        runCatching { speechRecognizer?.stopListening() }
        if (_uiState.value.busState == VoiceBusState.LISTENING) {
            if (!fallbackQuickPrompt.isNullOrBlank()) {
                submitUserPrompt(fallbackQuickPrompt)
            } else {
                setBusState(VoiceBusState.IDLE)
            }
        }
    }

    fun submitUserPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return

        val cfg = _uiState.value.config
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isFromUser = true,
            senderName = cfg.userName,
            text = prompt,
            timestampLabel = currentTimeLabel()
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                busState = VoiceBusState.THINKING
            )
        }

        viewModelScope.launch {
            delay(550L)
            handleAgentReply(prompt)
        }
    }

    private suspend fun handleAgentReply(prompt: String) {
        val lower = prompt.lowercase(Locale.US)
        val cfg = _uiState.value.config
        var presentedTitle: String? = null

        val replyText = when {
            lower.contains("hands free") || lower.contains("hands-free") -> {
                repository.saveConfig(cfg.copy(micMode = MicMode.HANDS_FREE))
                "Switched microphone to hands-free listening mode. I will keep listening until you switch back to push to talk."
            }

            lower.contains("push to talk") -> {
                repository.saveConfig(cfg.copy(micMode = MicMode.PUSH_TO_TALK))
                "Push to talk mode is active again. Hold the talk button whenever you want to speak."
            }

            lower.contains("stop asking") || lower.contains("auto approve") -> {
                repository.saveConfig(cfg.copy(permissionMode = PermissionMode.BYPASS_PERMISSIONS))
                "Understood. Auto-approve mode is now enabled for tool actions."
            }

            lower.contains("start asking") -> {
                repository.saveConfig(cfg.copy(permissionMode = PermissionMode.ASK))
                "Permission prompts restored. I will ask before running changes."
            }

            lower.startsWith("present ") || lower.contains("show me ") -> {
                val topic = prompt
                    .replace(Regex("^(present|show me)\\s+", RegexOption.IGNORE_CASE), "")
                    .trim()
                    .ifBlank { "Workspace Summary" }
                presentCardToBoard(
                    title = topic.replaceFirstChar { it.uppercase() },
                    body = "Pinned by ${cfg.agentName} via the present action. Active face: ${cfg.defaultFace.label}."
                )
                presentedTitle = topic
                if (StackPiece.HANDS_BOARD in cfg.enabledPieces) {
                    "Done — I placed \"$topic\" onto your spatial board using the present action."
                } else {
                    "I noted \"$topic\". Enable the Hands module in Setup if you want it pinned on the spatial board."
                }
            }

            lower.contains("fix") || lower.contains("repair") || lower.contains("broken") -> {
                repairAllWiring()
                "I inspected backtalk.json, ai-visualizer.json, and CLAUDE.md. Every config seam is aligned and healthy again."
            }

            lower.contains("update") -> {
                runStackUpdates()
                "Checked all installed repositories. Any available updates were applied without touching your vault notes or custom configs."
            }

            lower.contains("memory") || lower.contains("vault") || lower.contains("remember") -> {
                val count = _uiState.value.vaultNotes.size
                val titles = _uiState.value.vaultNotes.take(3).joinToString(", ") { it.title }
                "Your Memory Vault at ${cfg.vaultPath} currently holds $count Markdown notes ($titles)."
            }

            lower.contains("hello") || lower.contains("hi") || lower.contains("status") -> {
                "${cfg.greetingLine} Stack is running ${cfg.enabledPieces.size} active modules with ${cfg.defaultFace.label} face."
            }

            else -> {
                val matchingNote = _uiState.value.vaultNotes.firstOrNull { note ->
                    note.title.lowercase(Locale.US).split(" ").any { word ->
                        word.length > 3 && lower.contains(word)
                    } || note.content.lowercase(Locale.US).contains(lower.take(12))
                }
                if (matchingNote != null) {
                    "From your vault note '${matchingNote.title}': ${matchingNote.content}"
                } else {
                    "Got it, ${cfg.userName}. Logged under ${cfg.agentName}'s active session (${_uiState.value.launchMode.label} mode, ${cfg.micMode.configValue} mic). Say 'present <topic>' to pin anything to your board, or 'fix wiring' if a seam acts up."
                }
            }
        }

        appendAgentMessage(replyText, presentedTitle)
        if (StackPiece.VOICE_BACKTALK in cfg.enabledPieces) {
            speakText(replyText)
        } else {
            setBusState(VoiceBusState.IDLE)
        }
    }

    private fun appendAgentMessage(text: String, presentedTitle: String? = null) {
        val cfg = _uiState.value.config
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isFromUser = false,
            senderName = cfg.agentName,
            text = text,
            timestampLabel = currentTimeLabel(),
            presentedCardTitle = presentedTitle
        )
        _uiState.update { it.copy(messages = it.messages + msg) }
    }

    private fun speakText(text: String) {
        setBusState(VoiceBusState.SPEAKING)
        speakingTimeoutJob?.cancel()

        val engine = tts
        if (engine != null && _uiState.value.ttsReady) {
            val id = UUID.randomUUID().toString()
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        }

        val estimatedMs = (text.length * 48L).coerceIn(1400L, 6500L)
        speakingTimeoutJob = viewModelScope.launch {
            delay(estimatedMs)
            if (_uiState.value.busState == VoiceBusState.SPEAKING) {
                setBusState(VoiceBusState.IDLE)
            }
        }
    }

    // --- Memory Vault CRUD ---

    fun upsertVaultNote(existingId: String?, title: String, category: String, content: String) {
        viewModelScope.launch {
            val cleanTitle = title.trim().ifBlank { "Untitled Note" }
            val slug = cleanTitle.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "note" }
            val current = _uiState.value.vaultNotes
            val updated = if (existingId == null) {
                val created = VaultNote(
                    id = UUID.randomUUID().toString(),
                    fileName = "$slug.md",
                    title = cleanTitle,
                    category = category.ifBlank { "General" },
                    content = content.trim(),
                    updatedAtLabel = "Just now"
                )
                listOf(created) + current
            } else {
                current.map { note ->
                    if (note.id == existingId) {
                        note.copy(
                            fileName = "$slug.md",
                            title = cleanTitle,
                            category = category.ifBlank { "General" },
                            content = content.trim(),
                            updatedAtLabel = "Just now"
                        )
                    } else {
                        note
                    }
                }
            }
            repository.saveVaultNotes(updated)
            _uiState.update { it.copy(lastStatusBanner = "Saved $slug.md to Memory Vault") }
        }
    }

    fun deleteVaultNote(noteId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.vaultNotes.filterNot { it.id == noteId }
            repository.saveVaultNotes(updated)
            _uiState.update { it.copy(lastStatusBanner = "Removed note from Memory Vault") }
        }
    }

    // --- Spatial Hands Board CRUD ---

    fun presentCardToBoard(title: String, body: String) {
        viewModelScope.launch {
            val current = _uiState.value.boardCards
            val offsetIndex = current.size % 4
            val newCard = BoardCard(
                id = UUID.randomUUID().toString(),
                title = title.trim().ifBlank { "Pinned Card" },
                body = body.trim().ifBlank { "Presented onto the spatial board." },
                xFraction = (0.08f + offsetIndex * 0.14f).coerceAtMost(0.55f),
                yFraction = (0.14f + offsetIndex * 0.14f).coerceAtMost(0.62f),
                accentIndex = current.size % 3
            )
            repository.saveBoardCards(current + newCard)
            _uiState.update { it.copy(lastStatusBanner = "Presented '${newCard.title}' onto board") }
        }
    }

    fun moveBoardCard(cardId: String, xFraction: Float, yFraction: Float) {
        val clampedX = xFraction.coerceIn(0.02f, 0.62f)
        val clampedY = yFraction.coerceIn(0.04f, 0.72f)
        val updated = _uiState.value.boardCards.map { card ->
            if (card.id == cardId) card.copy(xFraction = clampedX, yFraction = clampedY) else card
        }
        _uiState.update { it.copy(boardCards = updated) }
    }

    fun persistBoardCards() {
        viewModelScope.launch {
            repository.saveBoardCards(_uiState.value.boardCards)
        }
    }

    fun removeBoardCard(cardId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.boardCards.filterNot { it.id == cardId }
            repository.saveBoardCards(updated)
        }
    }

    // --- Wiring, Config & Mechanic ---

    fun updateIdentityAndMechanic(
        agentName: String,
        userName: String,
        role: String,
        personality: String,
        mechanicRuleEnabled: Boolean
    ) {
        viewModelScope.launch {
            val updated = _uiState.value.config.copy(
                agentName = agentName.trim().ifBlank { "Atlas" },
                userName = userName.trim().ifBlank { "Builder" },
                agentRole = role.trim(),
                agentPersonality = personality.trim(),
                mechanicRuleEnabled = mechanicRuleEnabled
            )
            repository.saveConfig(updated)
            _uiState.update { it.copy(lastStatusBanner = "Updated CLAUDE.md identity") }
        }
    }

    fun simulateBrokenWiring() {
        viewModelScope.launch {
            val broken = _uiState.value.config.copy(wiringHealthy = false)
            repository.saveConfig(broken)
            _uiState.update {
                it.copy(
                    busState = VoiceBusState.IDLE,
                    lastStatusBanner = "Simulated stale bus_dir — face locked at idle until repaired."
                )
            }
        }
    }

    fun repairAllWiring() {
        viewModelScope.launch {
            val cfg = _uiState.value.config
            val repaired = cfg.copy(
                wiringHealthy = true,
                mechanicRuleEnabled = true,
                obsidianRegistered = true,
                busDir = "${cfg.agentHomeDir}/backtalk",
                barehandsStateDir = "${cfg.agentHomeDir}/barehands/state"
            )
            repository.saveConfig(repaired)
            _uiState.update {
                it.copy(lastStatusBanner = "Mechanic repaired all wiring seams. Configs verified.")
            }
        }
    }

    fun toggleMarketingSkills(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.config.copy(marketingSkillsInstalled = enabled)
            repository.saveConfig(updated)
            _uiState.update {
                it.copy(
                    lastStatusBanner = if (enabled) {
                        "Marketing playbook skills added to vault."
                    } else {
                        "Marketing playbook skills removed."
                    }
                )
            }
        }
    }

    fun runStackUpdates() {
        val currentRepos = _uiState.value.repoStatuses
        val updatedCount = currentRepos.count { it.installed && it.hasUpdate }
        val nextRepos = currentRepos.map { repo ->
            if (repo.installed && repo.hasUpdate) {
                repo.copy(
                    currentVersion = repo.latestVersion,
                    hasUpdate = false,
                    changelogSummary = "Updated cleanly to ${repo.latestVersion}. Local edits preserved."
                )
            } else {
                repo
            }
        }
        val msg = if (updatedCount > 0) {
            "Updated $updatedCount piece(s). Your CLAUDE.md, vault notes, and JSON configs stayed untouched."
        } else {
            "Everything is already up to date."
        }
        _uiState.update {
            it.copy(
                repoStatuses = nextRepos,
                updateBanner = msg,
                lastStatusBanner = msg
            )
        }
    }

    fun getGeneratedClaudeMd(): String = repository.generateClaudeMd(_uiState.value.config)
    fun getGeneratedBacktalkJson(): String = repository.generateBacktalkJson(_uiState.value.config)
    fun getGeneratedVisualizerJson(): String = repository.generateVisualizerJson(_uiState.value.config)
    fun getGeneratedBarehandsJson(): String = repository.generateBarehandsJson(_uiState.value.config)

    fun clearBanner() {
        _uiState.update { it.copy(lastStatusBanner = null) }
    }

    private fun currentTimeLabel(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    override fun onCleared() {
        super.onCleared()
        busAnimationJob?.cancel()
        speakingTimeoutJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
