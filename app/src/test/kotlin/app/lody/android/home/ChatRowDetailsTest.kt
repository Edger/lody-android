package app.lody.android.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRowDetailsTest {
    @Test
    fun rowsStartSingleLineWithOnlyThePullRequestDetail() {
        assertEquals(setOf(ChatRowDetail.PullRequest), ChatRowDetails.defaults)
    }
}
