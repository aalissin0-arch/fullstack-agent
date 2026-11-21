package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AgentStackConfig
import com.example.model.BoardCard
import com.example.model.StackPiece
import com.example.model.StackRepoStatus
import com.example.model.TroubleshootingItem
import com.example.model.VaultNote
import com.example.model.WiringDiagnostic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.io.File

private val Context.agentDataStore by preferencesDataStore(name = "fullstack_agent_store")

class AgentRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val configKey = stringPreferencesKey("agent_stack_config")
    private val notesKey = stringPreferencesKey("vault_notes")
    private val boardCardsKey = stringPreferencesKey("barehands_board_cards")

    val configFlow: Flow<AgentStackConfig> = context.agentDataStore.data.map { prefs ->
        val raw = prefs[configKey]
        if (raw.isNullOrBlank()) {
            AgentStackConfig()
        } else {
            runCatching { json.decodeFromString<AgentStackConfig>(raw) }
                .getOrDefault(AgentStackConfig())
        }
    }

    val vaultNotesFlow: Flow<List<VaultNote>> = context.agentDataStore.data.map { prefs ->
        val raw = prefs[notesKey]
        if (raw.isNullOrBlank()) {
            defaultVaultNotes()
        } else {
            runCatching { json.decodeFromString<List<VaultNote>>(raw) }
                .getOrDefault(defaultVaultNotes())
        }
    }

    val boardCardsFlow: Flow<List<BoardCard>> = context.agentDataStore.data.map { prefs ->
        val raw = prefs[boardCardsKey]
        if (raw.isNullOrBlank()) {
            defaultBoardCards()
        } else {
            runCatching { json.decodeFromString<List<BoardCard>>(raw) }
                .getOrDefault(defaultBoardCards())
        }
    }

    suspend fun saveConfig(config: AgentStackConfig) {
        context.agentDataStore.edit { prefs ->
            prefs[configKey] = json.encodeToString(config)
        }
        syncConfigFilesToDisk(config)
    }

    suspend fun saveVaultNotes(notes: List<VaultNote>) {
        context.agentDataStore.edit { prefs ->
            prefs[notesKey] = json.encodeToString(notes)
        }
        syncVaultNotesToDisk(notes)
    }

    suspend fun saveBoardCards(cards: List<BoardCard>) {
        context.agentDataStore.edit { prefs ->
            prefs[boardCardsKey] = json.encodeToString(cards)
        }
    }

    fun writeVoiceBusFiles(stateToken: String, waveformEnergy: Float) {
        runCatching {
            val busDir = File(context.filesDir, "agent-home/backtalk")
            if (!busDir.exists()) busDir.mkdirs()
            File(busDir, ".voice_state").writeText(stateToken)
            File(busDir, ".voice_waveform").writeText("%.3f".format(waveformEnergy))
        }
    }

    private fun syncConfigFilesToDisk(config: AgentStackConfig) {
        runCatching {
            val homeDir = File(context.filesDir, "agent-home")
            homeDir.mkdirs()
            File(homeDir, "CLAUDE.md").writeText(generateClaudeMd(config))

            val backtalkDir = File(homeDir, "backtalk")
            backtalkDir.mkdirs()
            File(backtalkDir, "backtalk.json").writeText(generateBacktalkJson(config))

            val visualizerDir = File(homeDir, "ai-visualizer")
            visualizerDir.mkdirs()
            File(visualizerDir, "ai-visualizer.json").writeText(generateVisualizerJson(config))

            val barehandsDir = File(homeDir, "barehands")
            barehandsDir.mkdirs()
            File(barehandsDir, "barehands.json").writeText(generateBarehandsJson(config))
        }
    }

    private fun syncVaultNotesToDisk(notes: List<VaultNote>) {
        runCatching {
            val vaultDir = File(context.filesDir, "agent-home/vault")
            vaultDir.mkdirs()
            notes.forEach { note ->
                File(vaultDir, note.fileName).writeText("# ${note.title}\n\n${note.content}")
            }
        }
    }

    fun generateClaudeMd(config: AgentStackConfig): String {
        val hasHands = StackPiece.HANDS_BOARD in config.enabledPieces
        val hasMemory = StackPiece.MEMORY_VAULT in config.enabledPieces
        return buildString {
            appendLine("# ${config.agentName}")
            appendLine()
            appendLine("Home folder: `${config.agentHomeDir}`")
            appendLine("User: ${config.userName}")
            appendLine("Role: ${config.agentRole}")
            appendLine("Personality: ${config.agentPersonality}")
            appendLine("Opening line: \"${config.greetingLine}\"")
            appendLine()
            if (hasMemory) {
                appendLine("## Memory Vault")
                appendLine("Persistent notes live at `${config.vaultPath}` (registered in Obsidian: ${config.obsidianRegistered}).")
                appendLine()
            }
            if (hasHands) {
                appendLine("## Spatial Board (barehands)")
                appendLine("When ${config.userName} asks to SEE something, put it on the glass board using the `present` action instead of answering only in text.")
                appendLine()
            }
            if (config.mechanicRuleEnabled) {
                appendLine("## You are the mechanic")
                appendLine("This agent runs on open stack modules inside `${config.agentHomeDir}` (memory vault, backtalk, ai-visualizer, barehands). When anything breaks, acts strange, or needs changing, diagnosing and repairing config seams is your job — never send the user off to hunt manually.")
            }
        }
    }

    fun generateBacktalkJson(config: AgentStackConfig): String {
        val obj: JsonObject = buildJsonObject {
            put("agent_dir", JsonPrimitive(config.agentHomeDir))
            put("name", JsonPrimitive(config.agentName))
            put("greeting", JsonPrimitive(config.greetingLine))
            put("permission_mode", JsonPrimitive(config.permissionMode.configValue))
            put("mic_mode", JsonPrimitive(config.micMode.configValue))
            put("talk_key", JsonPrimitive(config.talkKeyLabel))
            put("voice_engine", JsonPrimitive(config.voiceEngine.configValue))
            put("extra_dirs", buildJsonArray { add(JsonPrimitive(config.vaultPath)) })
            if (StackPiece.HANDS_BOARD in config.enabledPieces) {
                put("barehands_state_dir", JsonPrimitive(config.barehandsStateDir))
            }
        }
        return json.encodeToString(obj)
    }

    fun generateVisualizerJson(config: AgentStackConfig): String {
        val effectiveBusDir = if (config.wiringHealthy) config.busDir else "~/stale-path/backtalk"
        val obj: JsonObject = buildJsonObject {
            put("name", JsonPrimitive(config.agentName))
            put("face", JsonPrimitive(config.defaultFace.id))
            put("bus_dir", JsonPrimitive(effectiveBusDir))
            put("port", JsonPrimitive(8790))
        }
        return json.encodeToString(obj)
    }

    fun generateBarehandsJson(config: AgentStackConfig): String {
        val obj: JsonObject = buildJsonObject {
            put("name", JsonPrimitive(config.agentName))
            put("state_dir", JsonPrimitive(config.barehandsStateDir))
            put("port", JsonPrimitive(8794))
        }
        return json.encodeToString(obj)
    }

    fun runWiringDiagnostics(config: AgentStackConfig): List<WiringDiagnostic> {
        return listOf(
            WiringDiagnostic(
                id = "bus_seam",
                checkName = "Voice ↔ Face Bus Seam",
                passed = config.wiringHealthy && config.busDir.isNotBlank(),
                detail = if (config.wiringHealthy) {
                    "ai-visualizer.json bus_dir matches ${config.busDir} (.voice_state & .voice_waveform active)."
                } else {
                    "Mismatch: ai-visualizer.json points to ~/stale-path/backtalk instead of ${config.busDir}. Face will sit at idle."
                }
            ),
            WiringDiagnostic(
                id = "greeting_check",
                checkName = "Backtalk Launch Greeting",
                passed = config.greetingLine.isNotBlank(),
                detail = "Greeting configured: \"${config.greetingLine}\""
            ),
            WiringDiagnostic(
                id = "identity_home",
                checkName = "Home CLAUDE.md Identity",
                passed = config.agentName.isNotBlank() && config.agentHomeDir.isNotBlank(),
                detail = "${config.agentName} identity bound to ${config.agentHomeDir}/CLAUDE.md"
            ),
            WiringDiagnostic(
                id = "mechanic_clause",
                checkName = "Mechanic Self-Repair Clause",
                passed = config.mechanicRuleEnabled,
                detail = if (config.mechanicRuleEnabled) {
                    "Agent is authorized to read troubleshooting guides and repair config paths."
                } else {
                    "Mechanic directive disabled; append it to CLAUDE.md for self-healing."
                }
            ),
            WiringDiagnostic(
                id = "vault_registry",
                checkName = "Obsidian Vault Registry",
                passed = StackPiece.MEMORY_VAULT !in config.enabledPieces || config.obsidianRegistered,
                detail = if (StackPiece.MEMORY_VAULT in config.enabledPieces) {
                    "Vault registered at ${config.vaultPath} (opens directly without welcome prompt)."
                } else {
                    "Memory Vault module skipped; minimal CLAUDE.md brain active."
                }
            )
        )
    }

    fun initialRepoStatuses(config: AgentStackConfig): List<StackRepoStatus> = listOf(
        StackRepoStatus(
            repoName = "fullstack-agent",
            roleLabel = "Conductor & Launchers",
            installed = true,
            currentVersion = "v1.4.2",
            latestVersion = "v1.5.0",
            hasUpdate = true,
            changelogSummary = "Improved cross-piece seam checks and faster startup cleanup.",
            localConfigFile = null
        ),
        StackRepoStatus(
            repoName = "ai-memory-vault",
            roleLabel = "Mind & Markdown Vault",
            installed = StackPiece.MEMORY_VAULT in config.enabledPieces,
            currentVersion = "v2.1.0",
            latestVersion = "v2.1.0",
            hasUpdate = false,
            changelogSummary = "Up to date. Preserves your vault notes and CLAUDE.md.",
            localConfigFile = null
        ),
        StackRepoStatus(
            repoName = "backtalk",
            roleLabel = "Mouth & Voice Bus",
            installed = StackPiece.VOICE_BACKTALK in config.enabledPieces,
            currentVersion = "v1.8.0",
            latestVersion = "v1.8.3",
            hasUpdate = true,
            changelogSummary = "Lower push-to-talk latency and cleaner .voice_waveform smoothing.",
            localConfigFile = "backtalk.json"
        ),
        StackRepoStatus(
            repoName = "ai-visualizer",
            roleLabel = "Face Visualizer Gallery",
            installed = StackPiece.FACE_VISUALIZER in config.enabledPieces,
            currentVersion = "v1.3.1",
            latestVersion = "v1.3.1",
            hasUpdate = false,
            changelogSummary = "Up to date across Board, Radial, Rain, and Neural faces.",
            localConfigFile = "ai-visualizer.json"
        ),
        StackRepoStatus(
            repoName = "barehands",
            roleLabel = "Hands Spatial Board",
            installed = StackPiece.HANDS_BOARD in config.enabledPieces,
            currentVersion = "v1.1.0",
            latestVersion = "v1.2.0",
            hasUpdate = true,
            changelogSummary = "Snappier card pinning for the present verb.",
            localConfigFile = "barehands.json"
        )
    )

    fun troubleshootingCatalog(): List<TroubleshootingItem> = listOf(
        TroubleshootingItem(
            id = "face_idle",
            category = "Face & Voice Wiring",
            title = "The face sits at idle while the voice talks",
            symptom = "Agent speaks, but the visualizer chip stays dim and never transitions to Speaking.",
            resolution = "Verify ai-visualizer.json has bus_dir pointing at your backtalk folder (where .voice_state and .voice_waveform are written), then restart the visualizer.",
            autoFixable = true
        ),
        TroubleshootingItem(
            id = "silent_greeting",
            category = "Voice & Audio",
            title = "The greeting doesn't speak on launch",
            symptom = "Visualizer opens, but no spoken welcome line plays.",
            resolution = "Check that 'greeting' in backtalk.json is non-empty and Voice module is enabled in your stack.",
            autoFixable = true
        ),
        TroubleshootingItem(
            id = "agent_forgot",
            category = "Memory & Identity",
            title = "My agent forgot who it is",
            symptom = "Responses sound generic instead of using your agent's name and personality.",
            resolution = "Agent identity lives in the home folder's CLAUDE.md. Always launch sessions from the home directory, never inside a tool subfolder.",
            autoFixable = true
        ),
        TroubleshootingItem(
            id = "moved_folder",
            category = "Wiring & Paths",
            title = "I moved my agent folder somewhere else",
            symptom = "Seam paths broke after relocating the home directory.",
            resolution = "Re-run the Phase 4 wiring step to update agent_dir, bus_dir, extra_dirs, and barehands_state_dir in one pass.",
            autoFixable = true
        ),
        TroubleshootingItem(
            id = "port_busy",
            category = "Launchers",
            title = "Launcher says starting, but nothing appears",
            symptom = "Port 8790 (face) or 8794 (hands) is already occupied by an earlier session.",
            resolution = "Stop the previous stack cleanly before launching a new mode, or open the specific face view directly instead of the root picker.",
            autoFixable = false
        ),
        TroubleshootingItem(
            id = "interrupted_setup",
            category = "Setup Wizard",
            title = "Closed in the middle of setup",
            symptom = "Only some pieces were configured before exiting.",
            resolution = "Re-open the Setup Wizard from the Mechanic tab; existing identity and vault choices are preserved automatically without deleting prior work.",
            autoFixable = false
        )
    )

    private fun defaultVaultNotes(): List<VaultNote> = listOf(
        VaultNote(
            id = "note_profile",
            fileName = "user-profile.md",
            title = "User Profile & Preferences",
            category = "Identity",
            content = "Prefers plain-English explanations, clean modular architecture, and direct execution.",
            updatedAtLabel = "Today"
        ),
        VaultNote(
            id = "note_lessons",
            fileName = "lessons-learned.md",
            title = "System Wiring Lessons",
            category = "Lessons",
            content = "Keep bus_dir aligned with backtalk so .voice_state and .voice_waveform drive the active face in real time.",
            updatedAtLabel = "Today"
        ),
        VaultNote(
            id = "note_projects",
            fileName = "active-projects.md",
            title = "Active Projects",
            category = "Work",
            content = "- Fullstack Agent Android Edition\n- Voice + Living Circuit Board Stage\n- Spatial Hands Board Cards",
            updatedAtLabel = "Today"
        )
    )

    private fun defaultBoardCards(): List<BoardCard> = listOf(
        BoardCard(
            id = "card_stack",
            title = "Stack Status",
            body = "Mind + Voice + Face wired via .voice_state bus.",
            xFraction = 0.08f,
            yFraction = 0.12f,
            accentIndex = 0
        ),
        BoardCard(
            id = "card_present",
            title = "Present Verb",
            body = "Ask the agent to 'present' any topic to pin a card onto this glass board.",
            xFraction = 0.44f,
            yFraction = 0.46f,
            accentIndex = 1
        )
    )
}
