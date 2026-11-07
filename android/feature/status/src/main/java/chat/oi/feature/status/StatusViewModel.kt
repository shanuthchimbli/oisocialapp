package chat.oi.feature.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.oi.core.domain.model.PlainTextRules
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class StatusViewModel : ViewModel() {

    private val _viewState = MutableStateFlow(
        StatusViewState(
            myStatus = StatusItem(
                id = "stat_me",
                userId = "current_user_uuid",
                displayName = "Verified Citizen",
                username = "oi_user",
                text = "Focused on deep work — text only.",
                updatedAt = System.currentTimeMillis() - 1000 * 60 * 120
            ),
            contactsStatuses = listOf(
                StatusItem(
                    id = "stat_aarav",
                    userId = "user_aarav",
                    displayName = "Aarav Kumar",
                    username = "aarav_k",
                    text = "In meetings today. Reach out for urgent matters.",
                    updatedAt = System.currentTimeMillis() - 1000 * 60 * 45
                ),
                StatusItem(
                    id = "stat_eleanor",
                    userId = "user_eleanor",
                    displayName = "Eleanor Rigby",
                    username = "eleanor_r",
                    text = "Working remotely. Zero notifications enabled.",
                    updatedAt = System.currentTimeMillis() - 1000 * 60 * 60 * 6
                )
            )
        )
    )
    val viewState: StateFlow<StatusViewState> = _viewState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<StatusSideEffect>()
    val sideEffect: SharedFlow<StatusSideEffect> = _sideEffect.asSharedFlow()

    fun handleIntent(intent: StatusIntent) {
        when (intent) {
            is StatusIntent.UpdateDraft -> {
                _viewState.update {
                    it.copy(
                        statusDraft = intent.text,
                        errorMessage = if (
                            intent.text.codePointCount(0, intent.text.length) >
                            PlainTextRules.MAX_STATUS_CODE_POINTS
                        ) {
                            "ERR_INVALID_STATUS_LENGTH"
                        } else if (!PlainTextRules.isPlainTextStatus(intent.text)) {
                            "ERR_INVALID_STATUS_CONTENT"
                        } else {
                            null
                        }
                    )
                }
            }
            is StatusIntent.SetEditing -> {
                _viewState.update {
                    it.copy(
                        isEditing = intent.isEditing,
                        statusDraft = if (intent.isEditing) it.myStatus?.text ?: "" else "",
                        errorMessage = null
                    )
                }
            }
            StatusIntent.SaveStatus -> saveStatus()
            StatusIntent.DeleteStatus -> deleteStatus()
            StatusIntent.ClearError -> _viewState.update { it.copy(errorMessage = null) }
        }
    }

    private fun saveStatus() {
        val draft = _viewState.value.statusDraft.trim()
        val errorMessage = when {
            draft.isBlank() || draft.codePointCount(0, draft.length) >
                PlainTextRules.MAX_STATUS_CODE_POINTS -> "ERR_INVALID_STATUS_LENGTH"
            !PlainTextRules.isPlainTextStatus(draft) -> "ERR_INVALID_STATUS_CONTENT"
            else -> null
        }
        if (errorMessage != null) {
            _viewState.update { it.copy(errorMessage = errorMessage) }
            return
        }

        val newStatus = StatusItem(
            id = UUID.randomUUID().toString(),
            userId = "current_user_uuid",
            displayName = "Verified Citizen",
            username = "oi_user",
            text = draft,
            updatedAt = System.currentTimeMillis()
        )

        _viewState.update {
            it.copy(
                myStatus = newStatus,
                isEditing = false,
                statusDraft = ""
            )
        }

        viewModelScope.launch {
            _sideEffect.emit(StatusSideEffect.ShowToast("Status updated. Persists until changed."))
        }
    }

    private fun deleteStatus() {
        _viewState.update {
            it.copy(
                myStatus = null,
                isEditing = false,
                statusDraft = ""
            )
        }

        viewModelScope.launch {
            _sideEffect.emit(StatusSideEffect.ShowToast("Status deleted."))
        }
    }
}
