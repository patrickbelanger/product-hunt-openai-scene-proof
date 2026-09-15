package dev.sceneproof.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.ArrayDeque

@Component
class RequestAdmission(
    @param:Value("\${sceneproof.http.reads-per-minute:600}") private val reads: Int,
    @param:Value("\${sceneproof.http.writes-per-minute:120}") private val writes: Int,
    @param:Value("\${sceneproof.http.media-per-minute:30}") private val media: Int,
) {
    private val buckets = List(3) { ArrayDeque<Long>() }
    init { require(reads > 0 && writes > 0 && media > 0) }

    @Synchronized
    internal fun accept(mutation: Boolean, expensive: Boolean, now: Long = System.nanoTime()): Boolean {
        val index = if (expensive) 2 else if (mutation) 1 else 0
        val bucket = buckets[index]
        while (bucket.isNotEmpty() && now - bucket.first >= 60_000_000_000L) bucket.removeFirst()
        if (bucket.size >= listOf(reads, writes, media)[index]) return false
        bucket.addLast(now)
        return true
    }
}
