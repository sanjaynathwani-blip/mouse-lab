// Ported from OpenMouse's rotation-math.test.ts, plus a check that the incremental splitter
// gives the same answer however the samples are fed.
package app.mouselab.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class RotationMathTest {
    private fun swipes(angleDeg: Double, count: Int): List<RotationSample> {
        val rad = angleDeg * Math.PI / 180
        val out = ArrayList<RotationSample>()
        for (i in 0 until count) {
            val sign = if (i % 2 == 0) 1 else -1
            repeat(20) { out.add(RotationSample(sign * 10 * cos(rad), sign * 10 * sin(rad))) }
        }
        return out
    }

    @Test fun levelSwipesNeedNoCorrection() {
        assertEquals(RotationResult(12, 0.0, 0.0, 0.0), analyzeSwipes(swipes(0.0, 12)))
    }

    @Test fun clockwiseTiltIsCorrectedNegative() {
        assertEquals(-7.0, analyzeSwipes(swipes(7.0, 12))!!.correction, 0.0)
    }

    @Test fun counterClockwiseTiltIsCorrectedPositive() {
        assertEquals(4.5, analyzeSwipes(swipes(-4.5, 12))!!.correction, 0.0)
    }

    @Test fun nullWithNoMovement() {
        assertNull(analyzeSwipes(emptyList<RotationSample>()))
    }

    @Test fun ignoresJitterWhenCounting() {
        val jitter = List(30) { i -> RotationSample(if (i % 2 == 1) 1.0 else -1.0, 0.0) }
        assertEquals(0, countSwipes(jitter))
    }

    @Test fun oneWayMotionWithSmallBackwardJitterIsOneSwipe() {
        val samples = List(20) { i -> RotationSample(if (i % 2 == 0) 50.0 else -1.0, 0.0) }
        assertEquals(1, countSwipes(samples))
    }

    @Test fun identicalSwipesHaveNoSpread() {
        val result = analyzeSwipes(swipes(7.0, 12))!!
        assertEquals(0.0, result.spread, 0.0)
        assertEquals(0.0, result.margin, 0.0)
    }

    @Test fun scatteredSwipesReportSpreadAndShrinkingMargin() {
        fun scattered(count: Int): List<RotationSample> =
            (0 until count).flatMap { i ->
                val one = swipes(5.0 + if (i % 2 == 0) 2 else -2, 1)
                if (i % 2 == 0) one else one.map { RotationSample(-it.dx, -it.dy) }
            }
        val few = analyzeSwipes(scattered(10))!!
        val many = analyzeSwipes(scattered(40))!!
        assertEquals(-5.0, few.correction, 0.0)
        assertEquals(2.0, few.spread, 0.0)
        assertTrue(many.margin < few.margin)
    }

    @Test fun batchingDoesNotChangeTheResult() {
        val fine = swipes(6.0, 12)
        val coarse = fine.chunked(4).map { chunk -> RotationSample(chunk.sumOf { it.dx }, chunk.sumOf { it.dy }) }
        assertEquals(analyzeSwipes(fine), analyzeSwipes(coarse))
    }

    @Test fun liveCountMatchesTheFinalCount() {
        val samples = swipes(3.0, 15)
        val splitter = StrokeSplitter()
        samples.forEach { splitter.push(it.dx, it.dy) }
        assertEquals(15, splitter.count)
        assertEquals(analyzeSwipes(samples), analyzeSwipes(splitter))
    }
}
