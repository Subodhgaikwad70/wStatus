package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaManager
import com.example.model.MediaType
import com.example.model.SortOption
import com.example.model.StatusItem
import com.example.ui.StatusUiState
import com.example.ui.StatusViewModel
import com.example.ui.components.StatusCard
import com.example.ui.components.StorageAnalyzerDialog

@Composable
fun SavedVaultScreen(
    state: StatusUiState,
    viewModel: StatusViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var shareItem by remember { mutableStateOf<StatusItem?>(null) }

    // Filter by search query
    val searchFiltered = if (state.savedSearchQuery.isBlank()) {
        state.savedStatuses
    } else {
        state.savedStatuses.filter {
            it.title.contains(state.savedSearchQuery, ignoreCase = true) ||
            it.albumTag.contains(state.savedSearchQuery, ignoreCase = true)
        }
    }

    // Filter by album
    val albumFiltered = if (state.savedSelectedAlbum == "All") {
        searchFiltered
    } else {
        searchFiltered.filter { it.albumTag.equals(state.savedSelectedAlbum, ignoreCase = true) }
    }

    // Sort items
    val sortedList = when (state.savedSortOption) {
        SortOption.NEWEST -> albumFiltered.sortedByDescending { it.dateModified }
        SortOption.OLDEST -> albumFiltered.sortedBy { it.dateModified }
        SortOption.LARGEST_SIZE -> albumFiltered.sortedByDescending { it.sizeBytes }
        SortOption.SMALLEST_SIZE -> albumFiltered.sortedBy { it.sizeBytes }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("saved_vault_screen")
    ) {
        // Storage Bar & Analyzer banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Built-in Vault File Manager",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.savedStatuses.size} Saved • ${state.storageStats?.formattedTotalSize ?: "0 MB"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.savedStatuses.isNotEmpty()) {
                    Button(
                        onClick = { MediaManager.shareMultiple(context, state.savedStatuses) },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.WhatsAppGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("vault_share_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share All", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Button(
                    onClick = { showStorageDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("open_storage_analyzer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analyzer", fontSize = 11.sp)
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = state.savedSearchQuery,
            onValueChange = { viewModel.setSavedSearchQuery(it) },
            placeholder = { Text("Search by name or album tag...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (state.savedSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSavedSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("saved_search_input")
        )

        // Album Tag filter chips & Sort Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availableAlbums.forEach { album ->
                    val isSelected = state.savedSelectedAlbum == album
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSavedSelectedAlbum(album) },
                        label = {
                            Text(
                                text = album,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("album_chip_$album")
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Menu Button
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("sort_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.title,
                                    fontWeight = if (state.savedSortOption == option) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.setSavedSortOption(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Selection Action Bar for Saved Media
        if (state.isSelectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.clearSelection() },
                        modifier = Modifier.testTag("saved_clear_selection")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }

                    Text(
                        text = "${state.selectedIds.size} Selected",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { viewModel.selectAll() },
                        modifier = Modifier.testTag("saved_select_all")
                    ) {
                        Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Select All")
                    }

                    IconButton(
                        onClick = {
                            val itemsToShare = state.savedStatuses.filter { state.selectedIds.contains(it.id) }
                            MediaManager.shareMultiple(context, itemsToShare)
                        },
                        modifier = Modifier.testTag("saved_batch_share")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("saved_batch_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Main Grid of Saved items
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (sortedList.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .testTag("empty_saved_view")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (state.savedSearchQuery.isNotEmpty()) "No Matching Media" else "Vault is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (state.savedSearchQuery.isNotEmpty())
                            "Try searching for another keyword or clearing filters."
                        else
                            "Go to the 'Statuses' tab and tap Download on any status to save it here!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("saved_statuses_grid")
                ) {
                    items(
                        items = sortedList,
                        key = { it.id }
                    ) { item ->
                        val isSelected = state.selectedIds.contains(item.id)
                        StatusCard(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = state.isSelectionMode,
                            onItemClick = {
                                if (state.isSelectionMode) {
                                    viewModel.toggleSelection(item.id)
                                } else {
                                    viewModel.openPreview(item, sortedList)
                                }
                            },
                            onItemLongClick = {
                                viewModel.toggleSelection(item.id)
                            },
                            onSaveClick = {},
                            onToggleFavorite = {
                                viewModel.toggleFavorite(item)
                            },
                            onShareClick = {
                                shareItem = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Batch Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ${state.selectedIds.size} Items?") },
            text = { Text("This will remove the selected statuses from your device storage and Gallery permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedStatuses()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Storage Analyzer Dialog
    if (showStorageDialog) {
        StorageAnalyzerDialog(
            stats = state.storageStats,
            onClearCache = { viewModel.clearCache() },
            onDismiss = { showStorageDialog = false }
        )
    }

    shareItem?.let { item ->
        com.example.ui.components.QuickShareBottomSheet(
            item = item,
            onDismiss = { shareItem = null }
        )
    }
}
