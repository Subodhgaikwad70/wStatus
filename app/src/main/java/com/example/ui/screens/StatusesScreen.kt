package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaManager
import com.example.model.MediaType
import com.example.model.StatusItem
import com.example.model.WhatsAppAppType
import com.example.ui.MediaFilter
import com.example.ui.StatusUiState
import com.example.ui.StatusViewModel
import com.example.ui.components.StatusCard
import com.example.ui.theme.WhatsAppGreen

@Composable
fun StatusesScreen(
    state: StatusUiState,
    viewModel: StatusViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var shareItem by remember { mutableStateOf<StatusItem?>(null) }

    // SAF folder picker launcher
    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSafTreeUri(state.selectedAppType, uri)
        }
    }

    // Filter statuses based on selected mediaFilter
    val filteredList = state.statuses.filter { item ->
        when (state.mediaFilter) {
            MediaFilter.ALL -> true
            MediaFilter.IMAGES -> item.mediaType == MediaType.IMAGE
            MediaFilter.VIDEOS -> item.mediaType == MediaType.VIDEO
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("statuses_screen")
    ) {
        // Multi-App Switcher Tab Row
        ScrollableTabRow(
            selectedTabIndex = state.selectedAppType.ordinal,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedAppType.ordinal]),
                    color = WhatsAppGreen,
                    height = 3.dp
                )
            },
            divider = {}
        ) {
            WhatsAppAppType.entries.forEach { appType ->
                val selected = state.selectedAppType == appType
                Tab(
                    selected = selected,
                    onClick = { viewModel.selectAppType(appType) },
                    text = {
                        Text(
                            text = appType.displayName,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("app_tab_${appType.name}")
                )
            }
        }

        // Selection Mode Banner OR Filter Chips Row
        if (state.isSelectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
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
                        modifier = Modifier.testTag("clear_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel selection"
                        )
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
                        modifier = Modifier.testTag("select_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "Select All"
                        )
                    }

                    IconButton(
                        onClick = {
                            val itemsToShare = state.statuses.filter { state.selectedIds.contains(it.id) }
                            MediaManager.shareMultiple(context, itemsToShare)
                        },
                        modifier = Modifier.testTag("batch_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share selected"
                        )
                    }

                    Button(
                        onClick = { viewModel.saveSelectedStatuses() },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("batch_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save All", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Media Filter Chips Row (All, Photos, Videos) & Refresh
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    MediaFilter.entries.forEach { filter ->
                        val isFilterSelected = state.mediaFilter == filter
                        ElevatedFilterChip(
                            selected = isFilterSelected,
                            onClick = { viewModel.setMediaFilter(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.loadStatuses() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("refresh_statuses_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh statuses",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Grant Permission banner / Demo mode indicator
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reading ${state.selectedAppType.displayName} status folder",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { safFolderLauncher.launch(null) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("grant_folder_access_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Grant Folder", fontSize = 10.sp)
                }
            }
        }

        // Main Statuses Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (state.isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("loading_indicator")
                    )
                }
            } else if (filteredList.isEmpty()) {
                // Empty State with Tips
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .testTag("empty_statuses_view")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No ${state.mediaFilter.label} Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. Open ${state.selectedAppType.displayName}\n2. View your friends' statuses\n3. Tap Refresh or grant WhatsApp folder access below!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { safFolderLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_grant_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant Folder")
                        }

                        Button(
                            onClick = { viewModel.loadStatuses() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh")
                        }
                    }
                }
            } else {
                // Statuses Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("statuses_grid")
                ) {
                    items(
                        items = filteredList,
                        key = { it.id }
                    ) { statusItem ->
                        val isSelected = state.selectedIds.contains(statusItem.id)
                        StatusCard(
                            item = statusItem,
                            isSelected = isSelected,
                            isSelectionMode = state.isSelectionMode,
                            onItemClick = {
                                if (state.isSelectionMode) {
                                    viewModel.toggleSelection(statusItem.id)
                                } else {
                                    viewModel.openPreview(statusItem, filteredList)
                                }
                            },
                            onItemLongClick = {
                                viewModel.toggleSelection(statusItem.id)
                            },
                            onSaveClick = {
                                viewModel.saveStatus(statusItem)
                            },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(statusItem)
                            },
                            onShareClick = {
                                shareItem = statusItem
                            }
                        )
                    }
                }
            }
        }

        shareItem?.let { item ->
            com.example.ui.components.QuickShareBottomSheet(
                item = item,
                onDismiss = { shareItem = null }
            )
        }
    }
}
