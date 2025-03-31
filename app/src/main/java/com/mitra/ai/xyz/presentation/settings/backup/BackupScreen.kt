package com.mitra.ai.xyz.presentation.settings.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mitra.ai.xyz.domain.model.BackupComponent
import com.mitra.ai.xyz.domain.model.ConflictResolution
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // File pickers
    val backupFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.createBackup(it) }
    }
    
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    // Success/Error messages
    LaunchedEffect(state.success, state.error) {
        if (state.success != null || state.error != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Component selection
            Text(
                text = "Select components to backup",
                style = MaterialTheme.typography.titleMedium
            )
            
            BackupComponent.values().forEach { component ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = component in state.selectedComponents,
                        onCheckedChange = { checked ->
                            val newComponents = state.selectedComponents.toMutableSet()
                            if (checked) newComponents.add(component)
                            else newComponents.remove(component)
                            viewModel.setSelectedComponents(newComponents)
                        }
                    )
                    Text(
                        text = when(component) {
                            BackupComponent.SETTINGS -> "App Settings"
                            BackupComponent.PROVIDERS -> "AI Providers"
                            BackupComponent.CHATS -> "Chat History"
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Divider()

            // Conflict resolution
            Text(
                text = "Restore Conflict Resolution",
                style = MaterialTheme.typography.titleMedium
            )
            
            Column {
                ConflictResolution.values().forEach { resolution ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = resolution == state.conflictResolution,
                            onClick = { viewModel.setConflictResolution(resolution) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = when(resolution) {
                                    ConflictResolution.KEEP_EXISTING -> "Keep Existing Data"
                                    ConflictResolution.REPLACE_WITH_BACKUP -> "Replace with Backup"
                                    ConflictResolution.MERGE -> "Smart Merge"
                                }
                            )
                            Text(
                                text = when(resolution) {
                                    ConflictResolution.KEEP_EXISTING -> "Keep your current data, ignore backup"
                                    ConflictResolution.REPLACE_WITH_BACKUP -> "Replace all data with backup"
                                    ConflictResolution.MERGE -> "Merge data intelligently, keep newer versions"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Divider()

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        backupFilePicker.launch("mitra_backup.json")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.selectedComponents.isNotEmpty() && !state.isLoading
                ) {
                    Text("Create Backup")
                }
                
                Button(
                    onClick = {
                        restoreFilePicker.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading
                ) {
                    Text("Restore Backup")
                }
            }

            // Loading indicator
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Messages
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            state.success?.let { success ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = success,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
} 