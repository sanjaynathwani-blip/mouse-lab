package app.mouselab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mouselab.R
import app.mouselab.tools.PAUSE_MS
import app.mouselab.tools.PollingMeter
import app.mouselab.tools.PollingSnapshot
import app.mouselab.tools.nearestStandardRate
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

private class PollingState {
    var meter = PollingMeter()
    var dirty = false
}

private fun formatHz(hz: Double?): String = String.format(Locale.US, "%,d", (hz ?: 0.0).roundToInt())

@Composable
fun PollingScreen(wide: Boolean) {
    val state = remember { PollingState() }
    var snap by remember { mutableStateOf<PollingSnapshot?>(null) }
    var captured by remember { mutableStateOf(false) }
    var nonMouse by remember { mutableStateOf(false) }

    // Reports arrive at up to 8 kHz, so the readout is refreshed on a timer instead of per report.
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            if (state.dirty) {
                state.dirty = false
                snap = state.meter.snapshot()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Header(stringResource(R.string.poll_title), stringResource(R.string.poll_lead))

        CapturePad(
            height = 260.dp,
            active = captured,
            status = if (captured) {
                stringResource(R.string.poll_reports, String.format(Locale.US, "%,d", snap?.samples ?: 0))
            } else {
                stringResource(R.string.poll_idle)
            },
            footer = null,
            create = { pad ->
                pad.listener = object : CapturePadView.Listener {
                    override fun onCaptureChanged(hasCapture: Boolean) {
                        if (!hasCapture) state.meter.pause()
                        captured = hasCapture
                    }

                    override fun onReport(dx: Float, dy: Float, timeMs: Double) {
                        state.meter.push(timeMs)
                    }

                    override fun onBatchEnd() {
                        state.dirty = true
                    }

                    override fun onNonMouseInput() {
                        nonMouse = true
                    }
                }
            },
        )

        if (nonMouse) Note(stringResource(R.string.touchpad_hint), color = Lab.warn)

        val stats = listOf(
            stringResource(R.string.poll_current) to snap?.current,
            stringResource(R.string.poll_average) to snap?.average,
            stringResource(R.string.poll_peak) to snap?.peak,
        )
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.forEach { (label, value) -> ResultWell(label, formatHz(value), "Hz", Modifier.weight(1f)) }
            }
        } else {
            stats.forEach { (label, value) -> ResultWell(label, formatHz(value), "Hz", Modifier.fillMaxWidth()) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val nearest = snap?.let { nearestStandardRate(it.average) }
            Note(
                if (nearest != null) {
                    stringResource(R.string.poll_nearest, String.format(Locale.US, "%,d", nearest))
                } else {
                    stringResource(R.string.poll_note)
                },
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = {
                    state.meter = PollingMeter()
                    state.dirty = false
                    snap = null
                },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Lab.surface2, contentColor = Lab.text1),
                modifier = Modifier.height(36.dp),
            ) { Text(stringResource(R.string.poll_reset)) }
        }

        HowSection(
            title = stringResource(R.string.how_title),
            paragraphs = listOf(
                stringResource(R.string.poll_how_p1, PAUSE_MS.toInt()),
                stringResource(R.string.poll_how_p2),
            ),
        )
    }
}
