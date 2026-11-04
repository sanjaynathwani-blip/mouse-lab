// Ported from OpenMouse's polling-math.test.ts.
package app.mouselab.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class PollingMeterTest {
    private fun feed(meter: PollingMeter, intervalMs: Double, count: Int, start: Double = 0.0): Double {
        var t = start
        repeat(count) {
            meter.push(t)
            t += intervalMs
        }
        return t
    }

    @Test fun readsSteady1000Hz() {
        val meter = PollingMeter()
        feed(meter, 1.0, 400)
        val snap = meter.snapshot()!!
        assertEquals(1000, snap.average.roundToInt())
        assertEquals(1000, snap.current.roundToInt())
        assertEquals(1000, snap.peak.roundToInt())
    }

    @Test fun readsSteady125Hz() {
        val meter = PollingMeter()
        feed(meter, 8.0, 100)
        assertEquals(125, meter.snapshot()!!.average.roundToInt())
    }

    @Test fun pausesDoNotDragTheAverageDown() {
        val meter = PollingMeter()
        val end = feed(meter, 2.0, 100)
        feed(meter, 2.0, 100, end + 1500)
        assertEquals(500, meter.snapshot()!!.average.roundToInt())
    }

    @Test fun stoppingAndRestartingIsNotAGap() {
        val meter = PollingMeter()
        val end = feed(meter, 1.0, 100)
        meter.pause()
        feed(meter, 1.0, 100, end + 50)
        assertEquals(1000, meter.snapshot()!!.average.roundToInt())
    }

    @Test fun noReadingBeforeTwoReports() {
        val meter = PollingMeter()
        assertNull(meter.snapshot())
        meter.push(0.0)
        assertNull(meter.snapshot())
    }

    @Test fun snapsToAStandardRateOnlyWhenClose() {
        assertEquals(1000, nearestStandardRate(980.0))
        assertEquals(8000, nearestStandardRate(7600.0))
        assertNull(nearestStandardRate(700.0))
        assertNull(nearestStandardRate(0.0))
    }

    @Test fun peakNeverDrops() {
        val meter = PollingMeter()
        val end = feed(meter, 1.0, 400)
        val fast = meter.snapshot()!!.peak
        feed(meter, 8.0, 100, end)
        assertTrue(fast.roundToInt() >= 990)
        assertTrue(meter.snapshot()!!.peak >= fast)
    }

    @Test fun burstsSharingATimestampDoNotInflateThePeak() {
        // A true 1000 Hz mouse whose reports arrive in pairs with the same timestamp.
        val meter = PollingMeter()
        for (i in 0 until 400) {
            meter.push(i * 2.0)
            meter.push(i * 2.0)
        }
        val snap = meter.snapshot()!!
        assertTrue("peak ${snap.peak}", snap.peak <= 1010)
        assertTrue(snap.average <= 1010)
    }
}
