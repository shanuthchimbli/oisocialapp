package chat.oi.feature.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusViewModelTest {
    @Test
    fun updateDoesNotSilentlyTruncateOverlongStatus() {
        val viewModel = StatusViewModel()
        val draft = "🙂".repeat(141)

        viewModel.handleIntent(StatusIntent.UpdateDraft(draft))

        assertEquals(draft, viewModel.viewState.value.statusDraft)
        assertEquals("ERR_INVALID_STATUS_LENGTH", viewModel.viewState.value.errorMessage)

        viewModel.handleIntent(StatusIntent.UpdateDraft("Available today"))
        assertNull(viewModel.viewState.value.errorMessage)
    }

    @Test
    fun markdownStatusIsRejectedWithoutReplacingCurrentStatus() {
        val viewModel = StatusViewModel()
        val originalStatus = viewModel.viewState.value.myStatus
        viewModel.handleIntent(StatusIntent.SetEditing(true))
        viewModel.handleIntent(StatusIntent.UpdateDraft("[site](https://example.com)"))

        viewModel.handleIntent(StatusIntent.SaveStatus)

        assertEquals(originalStatus, viewModel.viewState.value.myStatus)
        assertEquals("ERR_INVALID_STATUS_CONTENT", viewModel.viewState.value.errorMessage)
    }
}
