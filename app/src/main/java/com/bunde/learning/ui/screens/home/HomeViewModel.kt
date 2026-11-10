package com.bunde.learning.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bunde.learning.BundeApplication
import com.bunde.learning.data.model.DevicePerformanceProfile
import com.bunde.learning.data.model.LearningStats
import com.bunde.learning.data.model.RevisionItem
import com.bunde.learning.data.model.Topic
import com.bunde.learning.util.DeviceOptimization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val studentName: String = "Learner",
    val classLevel: String = "SS2",
    val topics: List<Topic> = emptyList(),
    val continueTopic: Topic? = null,
    val revisionItems: List<RevisionItem> = emptyList(),
    val stats: LearningStats = LearningStats(),
    val deviceProfile: DevicePerformanceProfile? = null,
    val isApiKeyConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val appUpdateInfo: com.bunde.learning.util.AppUpdateInfo? = null,
    val isDownloadingUpdate: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BundeApplication
    private val storage = app.localStorage
    private val prefs = app.preferencesRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val profile = DeviceOptimization.getDeviceProfile(getApplication())

        viewModelScope.launch {
            prefs.studentNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(studentName = name)
            }
        }

        viewModelScope.launch {
            prefs.studentClassLevelFlow.collect { level ->
                _uiState.value = _uiState.value.copy(classLevel = level)
            }
        }

        viewModelScope.launch {
            prefs.geminiApiKeyFlow.collect { key ->
                _uiState.value = _uiState.value.copy(isApiKeyConfigured = key.isNotBlank())
            }
        }

        viewModelScope.launch {
            storage.topics.collect { topicsList ->
                val continueItem = topicsList.firstOrNull { !it.isMastered } ?: topicsList.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    topics = topicsList,
                    continueTopic = continueItem,
                    deviceProfile = profile
                )
            }
        }

        viewModelScope.launch {
            storage.revisionItems.collect { revList ->
                _uiState.value = _uiState.value.copy(revisionItems = revList)
            }
        }

        viewModelScope.launch {
            storage.stats.collect { statsData ->
                _uiState.value = _uiState.value.copy(stats = statsData)
            }
        }

        // Check for updates in the background on app launch
        checkForAppUpdates()
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            val updateInfo = com.bunde.learning.util.UpdateManager.checkForUpdate(getApplication())
            if (updateInfo.hasUpdate) {
                _uiState.value = _uiState.value.copy(appUpdateInfo = updateInfo)
            }
        }
    }

    fun triggerOneTapUpdate() {
        val info = _uiState.value.appUpdateInfo ?: return
        _uiState.value = _uiState.value.copy(isDownloadingUpdate = true)
        com.bunde.learning.util.UpdateManager.startOneTapDownloadAndInstall(
            context = getApplication(),
            downloadUrl = info.downloadUrl,
            versionName = info.latestVersionName
        )
    }

    fun dismissUpdateBanner() {
        _uiState.value = _uiState.value.copy(appUpdateInfo = null)
    }

    fun processRecallResult(item: RevisionItem, confidenceRating: Int) {
        viewModelScope.launch {
            val (nextInterval, isWeak) = when (confidenceRating) {
                1 -> Pair(1, true)
                2 -> Pair(3, false)
                3 -> Pair(7, false)
                else -> Pair(14, false)
            }

            val updated = item.copy(
                intervalDays = nextInterval,
                repetitionLevel = item.repetitionLevel + 1,
                dueDate = System.currentTimeMillis() + (nextInterval * 24 * 60 * 60 * 1000L),
                isWeakArea = isWeak
            )

            storage.updateRevisionItem(updated)
        }
    }
}
