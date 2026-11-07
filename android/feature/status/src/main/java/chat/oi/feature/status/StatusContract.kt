package chat.oi.feature.status

data class StatusItem(
    val id: String,
    val userId: String,
    val displayName: String,
    val username: String,
    val text: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class StatusViewState(
    val myStatus: StatusItem? = null,
    val contactsStatuses: List<StatusItem> = emptyList(),
    val statusDraft: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface StatusIntent {
    data class UpdateDraft(val text: String) : StatusIntent
    data object SaveStatus : StatusIntent
    data object DeleteStatus : StatusIntent
    data class SetEditing(val isEditing: Boolean) : StatusIntent
    data object ClearError : StatusIntent
}

sealed interface StatusSideEffect {
    data class ShowToast(val message: String) : StatusSideEffect
}
