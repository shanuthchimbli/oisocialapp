package chat.oi.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatViewModelTest {
    @Test
    fun overlongDraftIsKeptVisibleAndCannotBeSent() {
        val viewModel = ChatViewModel()
        val draft = "🙂".repeat(4097)
        viewModel.handleIntent(ChatIntent.SelectConversation("conversation", "contact"))

        viewModel.handleIntent(ChatIntent.UpdateDraft(draft))
        assertEquals(draft, viewModel.viewState.value.inputDraft)
        assertEquals("ERR_MESSAGE_TOO_LONG", viewModel.viewState.value.errorMessage)

        viewModel.handleIntent(ChatIntent.SendMessage)
        assertTrue(viewModel.viewState.value.messages.none { it.content == draft })
    }
}
