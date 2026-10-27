package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MediaManager
import com.example.ui.MainNavigationTab
import com.example.ui.StatusViewModel
import com.example.ui.components.MediaPreviewDialog
import com.example.ui.components.TutorialDialog
import com.example.ui.screens.SavedVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatusesScreen
import com.example.ui.theme.StatusSaverTheme
import com.example.ui.theme.WhatsAppGreen

class MainActivity : ComponentActivity() {
    private val viewModel: StatusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current

            LaunchedEffect(state.message) {
                state.message?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearMessage()
                }
            }

            StatusSaverTheme(themeMode = state.themeMode) {
                // Back handler when in sub-tab or selection mode
                BackHandler(enabled = state.isSelectionMode || state.currentTab != MainNavigationTab.STATUSES) {
                    if (state.isSelectionMode) {
                        viewModel.clearSelection()
                    } else if (state.currentTab != MainNavigationTab.STATUSES) {
                        viewModel.selectTab(MainNavigationTab.STATUSES)
                    }
                }

                Scaffold(
                    topBar = {
                        StatusSaverTopBar(
                            currentTab = state.currentTab,
                            onOpenTutorial = { viewModel.reopenTutorial() }
                        )
                    },
                    bottomBar = {
                        StatusSaverBottomNav(
                            currentTab = state.currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (state.currentTab) {
                            MainNavigationTab.STATUSES -> {
                                StatusesScreen(
                                    state = state,
                                    viewModel = viewModel
                                )
                            }
                            MainNavigationTab.SAVED -> {
                                SavedVaultScreen(
                                    state = state,
                                    viewModel = viewModel
                                )
                            }
                            MainNavigationTab.SETTINGS -> {
                                SettingsScreen(
                                    state = state,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // Full-screen media preview modal
                    state.previewItem?.let { previewItem ->
                        MediaPreviewDialog(
                            item = previewItem,
                            onDismiss = { viewModel.closePreview() },
                            onSave = { viewModel.saveStatus(it) },
                            onShare = { MediaManager.shareStatus(context, it) },
                            onRepost = { MediaManager.repostToWhatsApp(context, it) },
                            onDelete = { viewModel.deleteStatus(it) },
                            onSetAlbum = { item, tag -> viewModel.setAlbumTag(item, tag) },
                            availableAlbums = state.availableAlbums
                        )
                    }

                    // Tutorial Onboarding Modal
                    if (state.showTutorial) {
                        TutorialDialog(
                            onDismiss = { viewModel.setTutorialSeen() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSaverTopBar(
    currentTab: MainNavigationTab,
    onOpenTutorial: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WhatsAppGreen,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Status Saver",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onOpenTutorial,
                modifier = Modifier.testTag("topbar_tutorial_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "How to use",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun StatusSaverBottomNav(
    currentTab: MainNavigationTab,
    onTabSelected: (MainNavigationTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_navigation_bar")
    ) {
        MainNavigationTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            val icon = when (tab) {
                MainNavigationTab.STATUSES -> if (isSelected) Icons.Filled.Download else Icons.Outlined.Download
                MainNavigationTab.SAVED -> if (isSelected) Icons.Filled.Folder else Icons.Outlined.Folder
                MainNavigationTab.SETTINGS -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name}")
            )
        }
    }
}
