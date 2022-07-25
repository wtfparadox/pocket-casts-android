package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class StorageSettingsViewModel
@Inject constructor(
    private val fileStorage: FileStorage,
    private val fileUtil: FileUtilWrapper,
    private val settings: Settings,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableState = MutableStateFlow(initState())
    val state: StateFlow<State> = mutableState

    private val mutableSnackbarMessage = MutableSharedFlow<Int>()
    val snackbarMessage = mutableSnackbarMessage.asSharedFlow()

    private val storageChoiceSummary: String?
        get() = if (settings.usingCustomFolderStorage()) {
            context.getString(LR.string.settings_storage_custom_folder)
        } else {
            settings.getStorageChoiceName()
        }

    private fun initState() = State(
        storageDataWarningState = State.StorageDataWarningState(
            isChecked = settings.warnOnMeteredNetwork(),
            onCheckedChange = { onStorageDataWarningCheckedChange(it) }
        ),
        storageChoiceState = State.StorageChoiceState(
            title = settings.getStorageChoice(),
            summary = storageChoiceSummary
        )
    )

    fun onClearDownloadCacheClick() {
        viewModelScope.launch {
            val tempPath = fileStorage.tempPodcastDirectory
            fileUtil.deleteDirectoryContents(tempPath.absolutePath)
            mutableSnackbarMessage.emit(LR.string.settings_storage_clear_cache)
        }
    }

    private fun onStorageDataWarningCheckedChange(isChecked: Boolean) {
        settings.setWarnOnMeteredNetwork(isChecked)
        updateMobileDataWarningState()
    }

    private fun updateMobileDataWarningState() {
        mutableState.value = mutableState.value.copy(
            storageDataWarningState = mutableState.value.storageDataWarningState.copy(
                isChecked = settings.warnOnMeteredNetwork(),
            )
        )
    }

    data class State(
        val storageDataWarningState: StorageDataWarningState,
        val storageChoiceState: StorageChoiceState
    ) {
        data class StorageDataWarningState(
            val isChecked: Boolean = false,
            val onCheckedChange: (Boolean) -> Unit
        )

        data class StorageChoiceState(
            val title: String? = null,
            val summary: String? = null,
            val choices: Pair<Array<String?>, Array<String?>>? = null
        )
    }
}
