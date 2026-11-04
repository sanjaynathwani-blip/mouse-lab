// Ported from OpenMouse's polling-math.ts (openmouse-landing-page, AGPL-3.0).
package app.mouselab.tools

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

data class PollingSnapshot(
    val samples: Int,
    /** Reports per second over all active movement. */
    val average: Double,
    /** Reports per second over the most recent gaps. */
    val current: Double,
    /** Highest reading of [current] seen so far. */
    val peak: Double,
)

/** Gaps longer than this are pauses in movement, not the mouse's report rate. */
const val PAUSE_MS = 100.0

/** Peak is only read over at least this much movement, so bursts of reports with near-identical timestamps average out. */
private const val PEAK_WINDOW_MS = 250.0

/** Current needs at least this much movement before it reads anything. */
private const val CURRENT_WINDOW_MS = 50.0

private val STANDARD_RATES = intArrayOf(125, 250, 500, 1000, 2000, 4000, 8000)

/** Turns a stream of pointer report timestamps (ms) into polling rate readings. */
class PollingMeter {
    private var last: Double? = null
    private var activeMs = 0.0
    private var gapCount = 0
    private var samples = 0
    private var peak = 0.0

    // The most recent gaps, trimmed from the front once they cover more than PEAK_WINDOW_MS.
    private val gaps = ArrayDeque<Double>()
    private var windowMs = 0.0

    fun push(timestamp: Double) {
        samples += 1
        val previous = last
        if (previous != null) {
            val gap = timestamp - previous
            if (gap >= 0 && gap <= PAUSE_MS) {
                activeMs += gap
                gapCount += 1
                gaps.addLast(gap)
                windowMs += gap
                while (windowMs - gaps.first() >= PEAK_WINDOW_MS) windowMs -= gaps.removeFirst()
                if (windowMs >= PEAK_WINDOW_MS) peak = max(peak, windowRate())
            }
        }
        last = timestamp
    }

    /** Call when measuring stops, so the time away is not read as one long gap. */
    fun pause() {
        last = null
        gaps.clear()
        windowMs = 0.0
    }

    fun snapshot(): PollingSnapshot? {
        if (activeMs <= 0) return null
        return PollingSnapshot(
            samples = samples,
            average = 1000.0 * gapCount / activeMs,
            current = if (windowMs >= CURRENT_WINDOW_MS) windowRate() else 0.0,
            peak = peak,
        )
    }

    private fun windowRate(): Double = if (windowMs > 0) 1000.0 * gaps.size / windowMs else 0.0
}

/** The common polling rate closest to [hz], or null when none is within 15%. */
fun nearestStandardRate(hz: Double): Int? {
    if (hz <= 0) return null
    var best = STANDARD_RATES[0]
    for (candidate in STANDARD_RATES) {
        if (abs(ln(hz / candidate)) < abs(ln(hz / best))) best = candidate
    }
    return if (abs(hz - best) / best <= 0.15) best else null
}
