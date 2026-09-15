package dev.sceneproof

import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.analysis.PaidWorkGate
import dev.sceneproof.media.MediaIngestionGate
import dev.sceneproof.media.MediaStorage
import dev.sceneproof.project.ProjectDeletionService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.util.UUID

class ProjectDeletionGateTest {
    @Test fun `busy worker rejection never takes the media permit or touches project storage`() {
        val work = PaidWorkGate()
        val media = mock(MediaIngestionGate::class.java)
        val storage = mock(MediaStorage::class.java)
        val jdbc = mock(JdbcTemplate::class.java)
        val service = ProjectDeletionService(jdbc, mock(PlatformTransactionManager::class.java), storage, media, work)
        work.acquire()
        try {
            assertThatThrownBy { service.delete(UUID.randomUUID(), "fixture") }
                .isInstanceOf(AnalysisFailure::class.java)
                .hasMessage("PROJECT_BUSY")
            verifyNoInteractions(media, storage, jdbc)
            assertThatThrownBy { work.acquire() }.isInstanceOf(AnalysisFailure::class.java)
        } finally { work.release() }
    }
}
