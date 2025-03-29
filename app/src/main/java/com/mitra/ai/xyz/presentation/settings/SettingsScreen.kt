package com.mitra.ai.xyz.presentation.settings

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.mitra.ai.xyz.domain.model.AiProviderProfile
import org.burnoutcrew.reorderable.*
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.settingsState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<AiProviderProfile?>(null) }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val fromIndex = from.index - 1 // Subtract 1 to account for the header item
            val toIndex = to.index - 1 // Subtract 1 to account for the header item
            
            if (fromIndex >= 0 && toIndex >= 0 && 
                fromIndex < state.profiles.size && 
                toIndex < state.profiles.size) {
                val profiles = state.profiles.toMutableList()
                val item = profiles.removeAt(fromIndex)
                profiles.add(toIndex, item)
                viewModel.reorderProfiles(profiles)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add profile"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = reorderState.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(reorderState)
            ) {
                item {
                    Text(
                        text = "OpenAI Compatible Providers",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(
                    items = state.profiles,
                    key = { it.id }
                ) { profile ->
                    ReorderableItem(
                        reorderableState = reorderState,
                        key = profile.id
                    ) { isDragging ->
                        ProfileCard(
                            profile = profile,
                            onActivate = { viewModel.setActiveProfile(profile) },
                            onEdit = { profileToEdit = profile },
                            onDelete = { viewModel.deleteProfile(profile) },
                            modifier = Modifier
                                .detectReorderAfterLongPress(reorderState)
                                .animateItemPlacement()
                        )
                    }
                }

                if (state.profiles.isEmpty()) {
                    item {
                        EmptyState(
                            onAddClick = { showAddDialog = true }
                        )
                    }
                }
            }

            // Error snackbar
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Loading indicator
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    if (showAddDialog) {
        ProviderProfileDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, baseUrl, apiKey, model ->
                viewModel.addProfile(name, baseUrl, apiKey, model)
                showAddDialog = false
            }
        )
    }

    profileToEdit?.let { profile ->
        ProviderProfileDialog(
            onDismiss = { profileToEdit = null },
            onSave = { name, baseUrl, apiKey, model ->
                viewModel.updateProfile(
                    profile.copy(
                        name = name,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        model = model
                    )
                )
                profileToEdit = null
            },
            initialProfile = profile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: AiProviderProfile,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = profile.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!profile.isActive) {
                            DropdownMenuItem(
                                text = { Text("Set Active") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    onActivate()
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onEdit()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }

            if (profile.isActive) {
                AssistChip(
                    onClick = { },
                    label = { Text("Active") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "No Profiles Yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Add your first OpenAI-compatible provider",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        FilledTonalButton(
            onClick = onAddClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Profile")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderProfileDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, apiKey: String, model: String) -> Unit,
    initialProfile: AiProviderProfile? = null
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initialProfile?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf(initialProfile?.apiKey ?: "") }
    var model by remember { mutableStateOf(initialProfile?.model ?: "") }
    var showModelSelector by remember { mutableStateOf(false) }
    
    val viewModel: SettingsViewModel = hiltViewModel()
    var isLoadingModels by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var modelError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(apiKey, baseUrl) {
        if (apiKey.isNotBlank()) {
            isLoadingModels = true
            try {
                availableModels = viewModel.fetchAvailableModels(apiKey, baseUrl.takeIf { it.isNotBlank() })
                modelError = null
            } catch (e: Exception) {
                modelError = e.message
                availableModels = emptyList()
            }
            isLoadingModels = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProfile != null) "Edit Provider Profile" else "Add Provider Profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Model selector
                if (apiKey.isNotBlank()) {
                    Column {
                        OutlinedTextField(
                            value = model,
                            onValueChange = {},
                            label = { Text("Model") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showModelSelector = true }) {
                                    if (isLoadingModels) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select model"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (modelError != null) {
                            Text(
                                text = modelError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    if (showModelSelector && availableModels.isNotEmpty()) {
                        Dialog(
                            onDismissRequest = { showModelSelector = false }
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp)
                                ) {
                                    items(availableModels) { modelName ->
                                        ListItem(
                                            headlineContent = { Text(modelName) },
                                            modifier = Modifier.clickable {
                                                model = modelName
                                                showModelSelector = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, baseUrl, apiKey, model) },
                enabled = name.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
            ) {
                Text(if (initialProfile != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}