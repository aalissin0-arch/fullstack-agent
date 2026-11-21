package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class StackPiece(
    val repoName: String,
    val title: String,
    val plainSummary: String,
    val optionalAddon: Boolean = false
) {
    MEMORY_VAULT(
        repoName = "ai-memory-vault",
        title = "The Mind (Memory Vault)",
        plainSummary = "A filing cabinet of plain-text Markdown notes your agent reads and writes across every session."
    ),
    VOICE_BACKTALK(
        repoName = "backtalk",
        title = "The Mouth (Backtalk Voice)",
        plainSummary = "Hold a key or speak hands-free, and your agent replies out loud with its full personality."
    ),
    FACE_VISUALIZER(
        repoName = "ai-visualizer",
        title = "The Face (AI Visualizer)",
        plainSummary = "Living visualizers that idle, listen, think, and speak in real-time sync with your agent."
    ),
    HANDS_BOARD(
        repoName = "barehands",
        title = "The Hands (Spatial Board)",
        plainSummary = "Optional extra: arrange notes and presented cards on an interactive board.",
        optionalAddon = true
    )
}

@Serializable
enum class IdentityPreset(val title: String, val subtitle: String) {
    DEFAULT_ASSISTANT(
        title = "Door A: Classic Butler",
        subtitle = "Ready-to-use composed butler persona with crisp, helpful responses."
    ),
    CUSTOM_NAMED(
        title = "Door B: Classic Style, Your Name",
        subtitle = "Keep the calm, capable personality and rename the agent to anything you like."
    ),
    FROM_SCRATCH(
        title = "Door C: Build From Scratch",
        subtitle = "Define a custom agent name, role, and personality style in your own words."
    )
}

@Serializable
enum class MicMode(val configValue: String, val title: String, val description: String) {
    PUSH_TO_TALK(
        configValue = "ptt",
        title = "Push to Talk (Default)",
        description = "Mic stays closed until you hold the talk button, so background room audio never triggers it."
    ),
    HANDS_FREE(
        configValue = "open",
        title = "Hands-Free Listening",
        description = "Always listening without holding a key; room audio can trigger replies."
    )
}

@Serializable
enum class VoiceEngine(val configValue: String, val title: String, val description: String) {
    LOCAL_BUILTIN(
        configValue = "builtin",
        title = "Built-in Voice (Free & Offline)",
        description = "Runs locally on-device with zero network cost (default register: bm_lewis / system TTS)."
    ),
    ELEVEN_LABS(
        configValue = "elevenlabs",
        title = "ElevenLabs Neural Voice",
        description = "Natural human voice profile configured via your own ElevenLabs account."
    )
}

@Serializable
enum class PermissionMode(val configValue: String, val title: String, val description: String) {
    ASK(
        configValue = "ask",
        title = "Ask Out Loud First (Default)",
        description = "Agent confirms before editing files or running actions mid-conversation."
    ),
    BYPASS_PERMISSIONS(
        configValue = "bypassPermissions",
        title = "Auto-Approve Actions",
        description = "Agent executes tool actions automatically without pausing for verbal confirmation."
    )
}

@Serializable
enum class FaceStyle(val id: String, val label: String, val subtitle: String) {
    BOARD("board", "Board", "Living circuit board with agent chip"),
    RADIAL("radial", "Radial", "Harmonic concentric voice rings"),
    RAIN("rain", "Rain", "Ambient digital cascade streams"),
    NEURAL("neural", "Neural", "Synaptic graph cognition web")
}

@Serializable
enum class LaunchMode(val cliArg: String, val label: String, val description: String) {
    ALL("all", "Full Stack", "Face + Hands + Voice active"),
    VOICE_AND_FACE("voice", "Talk", "Voice and Face visualizer"),
    VOICE_AND_HANDS("hands", "Hands", "Voice and Spatial Board"),
    CHAT("chat", "Chat", "Typed terminal session")
}

enum class VoiceBusState(val stateToken: String, val label: String) {
    IDLE("idle", "Idle"),
    LISTENING("listening", "Listening"),
    THINKING("thinking", "Thinking"),
    SPEAKING("speaking", "Speaking")
}

@Serializable
data class AgentStackConfig(
    val setupCompleted: Boolean = false,
    val adoptionMode: Boolean = false,
    val userName: String = "Builder",
    val agentName: String = "Atlas",
    val identityPreset: IdentityPreset = IdentityPreset.DEFAULT_ASSISTANT,
    val agentRole: String = "Personal Full-Stack Operations Assistant",
    val agentPersonality: String = "Calm, resourceful, plain-English, and proactive about keeping tools healthy.",
    val enabledPieces: Set<StackPiece> = setOf(
        StackPiece.MEMORY_VAULT,
        StackPiece.VOICE_BACKTALK,
        StackPiece.FACE_VISUALIZER
    ),
    val vaultPath: String = "~/AgentVault",
    val usingExistingVault: Boolean = false,
    val obsidianRegistered: Boolean = true,
    val micMode: MicMode = MicMode.PUSH_TO_TALK,
    val talkKeyLabel: String = "Home / PTT Button",
    val voiceEngine: VoiceEngine = VoiceEngine.LOCAL_BUILTIN,
    val permissionMode: PermissionMode = PermissionMode.ASK,
    val defaultFace: FaceStyle = FaceStyle.BOARD,
    val busDir: String = "~/my-agent/backtalk",
    val barehandsStateDir: String = "~/my-agent/barehands/state",
    val agentHomeDir: String = "~/my-agent",
    val mechanicRuleEnabled: Boolean = true,
    val wiringHealthy: Boolean = true,
    val marketingSkillsInstalled: Boolean = false,
    val darkTheme: Boolean = true
) {
    val greetingLine: String
        get() = "Hello $userName, what are we working on today?"
}

@Serializable
data class VaultNote(
    val id: String,
    val fileName: String,
    val title: String,
    val category: String,
    val content: String,
    val updatedAtLabel: String
)

@Serializable
data class BoardCard(
    val id: String,
    val title: String,
    val body: String,
    val xFraction: Float,
    val yFraction: Float,
    val accentIndex: Int = 0
)

data class ChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val senderName: String,
    val text: String,
    val timestampLabel: String,
    val presentedCardTitle: String? = null
)

data class StackRepoStatus(
    val repoName: String,
    val roleLabel: String,
    val installed: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val hasUpdate: Boolean,
    val changelogSummary: String,
    val localConfigFile: String?
)

data class TroubleshootingItem(
    val id: String,
    val category: String,
    val title: String,
    val symptom: String,
    val resolution: String,
    val autoFixable: Boolean = false
)

data class WiringDiagnostic(
    val id: String,
    val checkName: String,
    val passed: Boolean,
    val detail: String
)
