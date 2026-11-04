// Ported from OpenMouse's rotation-math.ts (openmouse-landing-page, AGPL-3.0).
// The stroke splitter is made incremental so the live swipe count costs O(1) per report
// instead of re-splitting every sample; its output is the same as the original splitStrokes.
package app.mouselab.tools

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sign
import kotlin.math.sqrt

data class RotationSample(val dx: Double, val dy: Double)

data class RotationResult(
    val swipes: Int,
    /** Degrees to rotate the sensor output, clockwise positive, in [-45, 45]. */
    val correction: Double,
    /** Standard deviation of the individual swipe angles, in degrees. */
    val spread: Double,
    /** Standard error of the correction, in degrees: how far a repeat run is likely to differ. */
    val margin: Double,
)

const val MIN_SWIPES = 10

/** Enough swipes that hand scatter averages out to well under a degree. */
const val RECOMMENDED_SWIPES = 30

data class Stroke(val dx: Double, val dy: Double)

private fun toDegrees(rad: Double): Double = rad * 180.0 / Math.PI

/** Rounds to one decimal like JavaScript's Math.round (halves go up), and never returns -0. */
private fun round1(value: Double): Double {
    val r = floor(value * 10 + 0.5) / 10
    return if (r == 0.0) 0.0 else r
}

/**
 * Splits movement into strokes along the horizontal axis and keeps each one's net
 * displacement. A reversal only starts a new stroke once the move back covers [threshold];
 * shorter back-steps are jitter and stay part of the stroke they interrupt.
 */
class StrokeSplitter(private val threshold: Double = 40.0) {
    private val done = ArrayList<Stroke>()
    private var direction = 0.0
    private var run = 0.0
    private var netDx = 0.0
    private var netDy = 0.0
    private var reverse = 0.0
    private var backDx = 0.0
    private var backDy = 0.0

    /** True once any sample had movement. */
    var moved = false
        private set

    fun push(dx: Double, dy: Double) {
        if (dx != 0.0 || dy != 0.0) moved = true
        val s = sign(dx)
        if (s == 0.0) {
            if (reverse > 0) backDy += dy else netDy += dy
            return
        }
        if (direction == 0.0 || s == direction) {
            direction = s
            run += abs(dx)
            netDx += dx + backDx
            netDy += dy + backDy
            backDx = 0.0
            backDy = 0.0
            reverse = 0.0
            return
        }
        reverse += abs(dx)
        backDx += dx
        backDy += dy
        if (reverse < threshold) return
        if (run >= threshold) done.add(Stroke(netDx, netDy))
        direction = s
        run = reverse
        netDx = backDx
        netDy = backDy
        backDx = 0.0
        backDy = 0.0
        reverse = 0.0
    }

    val count: Int get() = done.size + if (run >= threshold) 1 else 0

    fun strokes(): List<Stroke> = if (run >= threshold) done + Stroke(netDx, netDy) else done.toList()
}

/** Count strokes along the horizontal axis. A reversal only counts once the move back covers [threshold]. */
fun countSwipes(samples: List<RotationSample>, threshold: Double = 40.0): Int {
    val splitter = StrokeSplitter(threshold)
    for ((dx, dy) in samples) splitter.push(dx, dy)
    return splitter.count
}

fun analyzeSwipes(samples: List<RotationSample>): RotationResult? {
    val splitter = StrokeSplitter()
    for ((dx, dy) in samples) splitter.push(dx, dy)
    return analyzeSwipes(splitter)
}

/**
 * Fit the axis the swipes travel along and return the rotation that levels it.
 * Each swipe is reduced to its net displacement, so how events are batched and the curve
 * of a turnaround do not skew the fit. Swipes alternate direction, so the fit uses the
 * second moments of those vectors (sign-free) rather than a line through the path.
 */
fun analyzeSwipes(splitter: StrokeSplitter): RotationResult? {
    if (!splitter.moved) return null

    val strokes = splitter.strokes()
    if (strokes.isEmpty()) return RotationResult(0, 0.0, 0.0, 0.0)

    var sxx = 0.0
    var syy = 0.0
    var sxy = 0.0
    for ((dx, dy) in strokes) {
        sxx += dx * dx
        syy += dy * dy
        sxy += dx * dy
    }

    // Screen y points down, so a positive tilt here means the swipes run clockwise of
    // horizontal. Levelling them rotates the output the other way, so the correction is
    // the negated tilt, in the clockwise-positive convention mouse software uses.
    val tilt = toDegrees(0.5 * atan2(2 * sxy, sxx - syy))
    // A mostly vertical stroke has no horizontal axis to level.
    if (abs(tilt) > 45) return RotationResult(strokes.size, 0.0, 0.0, 0.0)

    // Spread of the swipe angles around the fitted axis, weighted like the fit itself.
    var weight = 0.0
    var variance = 0.0
    for ((dx, dy) in strokes) {
        val angle = toDegrees(atan2(dy * sign(dx), abs(dx)))
        val w = dx * dx + dy * dy
        val d = angle - tilt
        variance += w * d * d
        weight += w
    }
    val spread = sqrt(variance / weight)
    val margin = if (strokes.size > 1) spread / sqrt(strokes.size.toDouble()) else spread
    val correction = -round1(tilt)
    return RotationResult(strokes.size, if (correction == 0.0) 0.0 else correction, round1(spread), round1(margin))
}
