package dev.sceneproof.analysis

import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class PaidRunAdmission(
    private val jdbc: JdbcTemplate,
    @param:Value("\${sceneproof.paid-runs.enabled:true}") private val enabled: Boolean,
    @param:Value("\${sceneproof.paid-runs.hourly-limit:10}") private val hourlyLimit: Int,
    @param:Value("\${sceneproof.paid-runs.daily-limit:100}") private val dailyLimit: Int,
) {
    init { require(hourlyLimit > 0 && dailyLimit > 0) }

    @Transactional(propagation = Propagation.MANDATORY)
    fun reserve(runId: UUID) {
        jdbc.execute("SELECT pg_advisory_xact_lock(731205)")
        if (jdbc.queryForObject("SELECT count(*) FROM paid_run_reservations WHERE run_id = ?", Long::class.java, runId) != 0L) return
        if (!enabled) throw AnalysisFailure("PAID_RUNS_DISABLED", "New paid runs are disabled by the operator. Saved results remain available.", 429)
        val counts = jdbc.queryForMap("SELECT count(*) FILTER (WHERE reserved_at > clock_timestamp() - INTERVAL '1 hour') AS hourly, count(*) AS daily FROM paid_run_reservations WHERE reserved_at > clock_timestamp() - INTERVAL '24 hours'")
        if ((counts.getValue("hourly") as Number).toLong() >= hourlyLimit || (counts.getValue("daily") as Number).toLong() >= dailyLimit) {
            throw AnalysisFailure("PAID_RUN_LIMIT", "The deployment's paid-run safety limit has been reached. No provider request was made. Try later without creating another project.", 429)
        }
        jdbc.update("DELETE FROM paid_run_reservations WHERE reserved_at <= clock_timestamp() - INTERVAL '24 hours'")
        jdbc.update("INSERT INTO paid_run_reservations (run_id) VALUES (?)", runId)
    }
}
