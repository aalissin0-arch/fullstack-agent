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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.StackPiece
import com.example.ui.AgentUiState
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun MechanicScreen(
    state: AgentUiState,
    onRunUpdates: () -> Unit,
    onRepairAll: () -> Unit,
    onOpenWizard: () -> Unit,
    onToggleMarketingSkills: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val cfg = state.config
    val anyDiagnosticFailed = state.diagnostics.any { !it.passed }
    val pendingUpdateCount = state.repoStatuses.count { it.installed && it.hasUpdate }

    val filteredTroubleshooting = remember(state.troubleshootingItems, searchQuery) {
        if (searchQuery.isBlank()) {
            state.troubleshootingItems
        } else {
            val q = searchQuery.lowercase()
            state.troubleshootingItems.filter {
                it.title.lowercase().contains(q) ||
                    it.symptom.lowercase().contains(q) ||
                    it.resolution.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Mechanic Diagnostics & One-Tap Repair
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                1.dp,
                if (anyDiagnosticFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stack Health & Diagnostics", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenWizard,
                            modifier = Modifier.testTag("mechanic_wizard_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wizard")
                        }
                    }
                }

                state.diagnostics.forEach { diag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (diag.passed) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (diag.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(diag.checkName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = diag.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (anyDiagnosticFailed) {
                    Button(
                        onClick = onRepairAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mechanic_repair_all_button")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Mechanic Repair")
                    }
                }
            }
        }

        // Section 2: Safe Stack Updater (update.sh)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Piece Updater (update.sh)", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Updates installed repos while preserving CLAUDE.md, vault notes, and local JSON configs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onRunUpdates,
                        modifier = Modifier.testTag("mechanic_update_button")
                    ) {
                        Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (pendingUpdateCount > 0) "Update ($pendingUpdateCount)" else "Current")
                    }
                }

                state.updateBanner?.let { banner ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = banner,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                state.repoStatuses.forEach { repo ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = repo.repoName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                Text(
                                    text = when {
                                        !repo.installed -> "NOT INSTALLED"
                                        repo.hasUpdate -> "${repo.currentVersion} -> ${repo.latestVersion}"
                                        else -> repo.currentVersion
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (repo.hasUpdate && repo.installed) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                            Text(
                                text = "${repo.roleLabel} · ${repo.changelogSummary}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Generated Launchers & Optional Playbook
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generated Launchers", style = MaterialTheme.typography.titleMedium)
                }
                val launcherLines = buildList {
                    add("Chat with ${cfg.agentName}  ->  cd ${cfg.agentHomeDir} && claude")
                    if (StackPiece.VOICE_BACKTALK in cfg.enabledPieces) {
                        add("Talk to ${cfg.agentName}    ->  ./fullstack-agent/start.sh voice")
                    }
                    if (StackPiece.HANDS_BOARD in cfg.enabledPieces) {
                        add("${cfg.agentName} Barehands   ->  ./fullstack-agent/start.sh hands")
                    }
                    add("Update ${cfg.agentName}      ->  ./fullstack-agent/update.sh")
                }
                launcherLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Optional Marketing Playbook Skills", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Adds funnel & copywriting workflows into your agent's vault skills.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cfg.marketingSkillsInstalled,
                        onCheckedChange = onToggleMarketingSkills,
                        modifier = Modifier.testTag("mechanic_marketing_switch")
                    )
                }
            }
        }

        // Section 4: Searchable Troubleshooting Guide (TROUBLESHOOTING.md)
        Text("Troubleshooting Guide", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search cross-piece issues…") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mechanic_troubleshoot_search")
        )

        filteredTroubleshooting.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (item.autoFixable) {
                            OutlinedButton(onClick = onRepairAll) {
                                Text("Auto-Fix")
                            }
                        }
                    }
                    Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Symptom: ${item.symptom}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Fix: ${item.resolution}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
