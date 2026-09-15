package dev.sceneproof.analysis

import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore

@Component
class PaidWorkGate {
    private val permit = Semaphore(1)
    fun acquire() {
        if (!permit.tryAcquire()) throw AnalysisFailure("ANALYSIS_BUSY", "Another analysis worker is still active. No new provider request was made.", 429)
    }
    fun release() = permit.release()
}
