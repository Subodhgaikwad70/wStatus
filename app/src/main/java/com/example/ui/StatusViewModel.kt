package com.example.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaManager
import com.example.data.StatusRepository
import com.example.data.StorageStats
import com.example.data.UserPreferences
import com.example.model.MediaType
import com.example.model.SortOption
import com.example.model.StatusItem
import com.example.model.ThemeMode
import com.example.model.WhatsAppAppType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainNavigationTab(val label: String) {
    STATUSES("Statuses"),
    SAVED("Saved Vault"),
    SETTINGS("Settings")
}

enum class MediaFilter(val label: String) {
    ALL("All"),
    IMAGES("Photos"),
    VIDEOS("Videos")
}

data class StatusUiState(
    val currentTab: MainNavigationTab = MainNavigationTab.STATUSES,
    val selectedAppType: WhatsAppAppType = WhatsAppAppType.WHATSAPP,
    val mediaFilter: MediaFilter = MediaFilter.ALL,
    val statuses: List<StatusItem> = emptyList(),
    val savedStatuses: List<StatusItem> = emptyList(),
    val isLoading: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showTutorial: Boolean = false,
    val demoMode: Boolean = true,
    val previewItem: StatusItem? = null,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val savedSearchQuery: String = "",
    val savedSelectedAlbum: String = "All",
    val savedSortOption: SortOption = SortOption.NEWEST,
    val availableAlbums: List<String> = listOf("All", "Travel", "Quotes", "Daily", "Tech", "Fitness", "Memes"),
    val storageStats: StorageStats? = null,
    val message: String? = null
)

class StatusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StatusRepository(application)
    private val preferences = UserPreferences(application)

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    init {
        // Observe preferences
        viewModelScope.launch {
            preferences.themeModeFlow.collectLatest { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        viewModelScope.launch {
            preferences.hasSeenTutorialFlow.collectLatest { seen ->
                // Show tutorial automatically if first time
                _uiState.update { it.copy(showTutorial = !seen) }
            }
        }

        viewModelScope.launch {
            preferences.demoModeFlow.collectLatest { demo ->
                _uiState.update { it.copy(demoMode = demo) }
                loadStatuses()
            }
        }

        loadStatuses()
        loadSavedStatuses()
        loadStorageStats()
    }

    fun selectTab(tab: MainNavigationTab) {
        _uiState.update { it.copy(currentTab = tab, isSelectionMode = false, selectedIds = emptySet()) }
        if (tab == MainNavigationTab.SAVED) {
            loadSavedStatuses()
            loadStorageStats()
        } else if (tab == MainNavigationTab.STATUSES) {
            loadStatuses()
        }
    }

    fun selectAppType(appType: WhatsAppAppType) {
        _uiState.update { it.copy(selectedAppType = appType, isSelectionMode = false, selectedIds = emptySet()) }
        loadStatuses()
    }

    fun setMediaFilter(filter: MediaFilter) {
        _uiState.update { it.copy(mediaFilter = filter) }
    }

    fun loadStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = repository.getStatuses(
                    appType = _uiState.value.selectedAppType,
                    forceDemo = _uiState.value.demoMode
                )
                _uiState.update { it.copy(statuses = list, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Failed to load statuses: ${e.message}") }
            }
        }
    }

    fun loadSavedStatuses() {
        viewModelScope.launch {
            try {
                val list = repository.getSavedStatuses()
                // Collect dynamic tags from saved items
                val albums = mutableSetOf("All")
                list.forEach { if (it.albumTag.isNotBlank()) albums.add(it.albumTag) }
                listOf("Travel", "Quotes", "Daily", "Tech", "Fitness", "Memes").forEach { albums.add(it) }

                _uiState.update {
                    it.copy(
                        savedStatuses = list,
                        availableAlbums = albums.toList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to load saved items") }
            }
        }
    }

    fun loadStorageStats() {
        viewModelScope.launch {
            val stats = repository.getStorageStats()
            _uiState.update { it.copy(storageStats = stats) }
        }
    }

    fun saveStatus(item: StatusItem) {
        viewModelScope.launch {
            val result = MediaManager.saveToGallery(getApplication(), item)
            result.onSuccess {
                Toast.makeText(getApplication(), "Saved to Gallery! 📥", Toast.LENGTH_SHORT).show()
                // Update item saved flag in statuses list
                _uiState.update { state ->
                    val updated = state.statuses.map {
                        if (it.id == item.id) it.copy(isSaved = true) else it
                    }
                    val updatedPreview = if (state.previewItem?.id == item.id) state.previewItem.copy(isSaved = true) else state.previewItem
                    state.copy(statuses = updated, previewItem = updatedPreview)
                }
                loadSavedStatuses()
                loadStorageStats()
            }.onFailure { err ->
                Toast.makeText(getApplication(), "Failed to save: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun saveSelectedStatuses() {
        val selected = _uiState.value.statuses.filter { _uiState.value.selectedIds.contains(it.id) }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            for (item in selected) {
                val res = MediaManager.saveToGallery(getApplication(), item)
                if (res.isSuccess) successCount++
            }
            Toast.makeText(getApplication(), "Saved $successCount items to Gallery!", Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }
            loadStatuses()
            loadSavedStatuses()
            loadStorageStats()
        }
    }

    fun deleteStatus(item: StatusItem) {
        viewModelScope.launch {
            val deleted = repository.deleteSavedStatus(item)
            if (deleted) {
                Toast.makeText(getApplication(), "Removed from Saved Vault", Toast.LENGTH_SHORT).show()
                if (_uiState.value.previewItem?.id == item.id) {
                    _uiState.update { it.copy(previewItem = null) }
                }
                loadSavedStatuses()
                loadStorageStats()
                loadStatuses()
            } else {
                Toast.makeText(getApplication(), "Could not delete item", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteSelectedStatuses() {
        val selected = _uiState.value.savedStatuses.filter { _uiState.value.selectedIds.contains(it.id) }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            var deletedCount = 0
            for (item in selected) {
                if (repository.deleteSavedStatus(item)) deletedCount++
            }
            Toast.makeText(getApplication(), "Deleted $deletedCount items", Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }
            loadSavedStatuses()
            loadStorageStats()
            loadStatuses()
        }
    }

    fun toggleFavorite(item: StatusItem) {
        viewModelScope.launch {
            preferences.toggleFavorite(item.id)
            val newFav = !item.isFavorite
            _uiState.update { state ->
                val updatedStatuses = state.statuses.map { if (it.id == item.id) it.copy(isFavorite = newFav) else it }
                val updatedSaved = state.savedStatuses.map { if (it.id == item.id) it.copy(isFavorite = newFav) else it }
                val updatedPreview = if (state.previewItem?.id == item.id) state.previewItem.copy(isFavorite = newFav) else state.previewItem
                state.copy(statuses = updatedStatuses, savedStatuses = updatedSaved, previewItem = updatedPreview)
            }
        }
    }

    fun setAlbumTag(item: StatusItem, tag: String) {
        viewModelScope.launch {
            preferences.setAlbumTag(item.id, tag)
            _uiState.update { state ->
                val updatedSaved = state.savedStatuses.map { if (it.id == item.id) it.copy(albumTag = tag) else it }
                val updatedPreview = if (state.previewItem?.id == item.id) state.previewItem.copy(albumTag = tag) else state.previewItem
                state.copy(savedStatuses = updatedSaved, previewItem = updatedPreview)
            }
            Toast.makeText(getApplication(), "Moved to \"$tag\" album", Toast.LENGTH_SHORT).show()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDemoMode(enabled)
        }
    }

    fun setTutorialSeen() {
        viewModelScope.launch {
            preferences.setTutorialSeen(true)
            _uiState.update { it.copy(showTutorial = false) }
        }
    }

    fun reopenTutorial() {
        _uiState.update { it.copy(showTutorial = true) }
    }

    fun openPreview(item: StatusItem) {
        _uiState.update { it.copy(previewItem = item) }
    }

    fun closePreview() {
        _uiState.update { it.copy(previewItem = null) }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val set = state.selectedIds.toMutableSet()
            if (set.contains(id)) {
                set.remove(id)
            } else {
                set.add(id)
            }
            state.copy(
                selectedIds = set,
                isSelectionMode = set.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = when (state.currentTab) {
                MainNavigationTab.STATUSES -> state.statuses.map { it.id }.toSet()
                MainNavigationTab.SAVED -> state.savedStatuses.map { it.id }.toSet()
                else -> emptySet()
            }
            state.copy(isSelectionMode = true, selectedIds = allIds)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }
    }

    fun setSavedSearchQuery(query: String) {
        _uiState.update { it.copy(savedSearchQuery = query) }
    }

    fun setSavedSelectedAlbum(album: String) {
        _uiState.update { it.copy(savedSelectedAlbum = album) }
    }

    fun setSavedSortOption(option: SortOption) {
        _uiState.update { it.copy(savedSortOption = option) }
    }

    fun clearCache() {
        viewModelScope.launch {
            val cleared = repository.clearCache()
            if (cleared) {
                Toast.makeText(getApplication(), "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                loadStorageStats()
            }
        }
    }

    fun setSafTreeUri(appType: WhatsAppAppType, uri: Uri) {
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            preferences.setSafUri(appType, uri.toString())
            Toast.makeText(getApplication(), "Folder access granted for ${appType.displayName}!", Toast.LENGTH_SHORT).show()
            loadStatuses()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
