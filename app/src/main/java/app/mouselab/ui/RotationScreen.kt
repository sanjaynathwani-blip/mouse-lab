package app.mouselab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mouselab.R
import app.mouselab.tools.MIN_SWIPES
import app.mouselab.tools.RECOMMENDED_SWIPES
import app.mouselab.tools.RotationResult
import app.mouselab.tools.StrokeSplitter
import app.mouselab.tools.analyzeSwipes
import java.util.Locale

private enum class Phase { Idle, Measuring, Done }

@Composable
fun RotationScreen(padHeight: androidx.compose.ui.unit.Dp) {
    var phase by remember { mutableStateOf(Phase.Idle) }
    var swipes by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<RotationResult?>(null) }
    var nonMouse by remember { mutableStateOf(false) }
    val splitter = remember { arrayOf(StrokeSplitter()) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Header(stringResource(R.string.rot_title), stringResource(R.string.rot_lead))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Note(stringResource(R.string.rot_step1), color = Lab.text2)
            Note(stringResource(R.string.rot_step2, RECOMMENDED_SWIPES), color = Lab.text2)
            Note(stringResource(R.string.rot_step3), color = Lab.text2)
        }

        CapturePad(
            height = padHeight,
            active = phase == Phase.Measuring,
            status = when (phase) {
                Phase.Idle -> stringResource(R.string.rot_idle)
                Phase.Measuring -> stringResource(R.string.rot_measuring, swipes)
                Phase.Done -> stringResource(R.string.rot_restart)
            },
            footer = if (phase == Phase.Measuring) stringResource(R.string.rot_stop) else null,
            // Once there is a trail, keep the status clear of it.
            statusOnTop = phase != Phase.Idle,
            create = { pad ->
                pad.drawTrail = true
                pad.listener = object : CapturePadView.Listener {
                    override fun onCaptureChanged(captured: Boolean) {
                        if (captured) {
                            splitter[0] = StrokeSplitter()
                            pad.clearTrail()
                            swipes = 0
                            result = null
                            phase = Phase.Measuring
                        } else if (phase == Phase.Measuring) {
                            result = analyzeSwipes(splitter[0])
                            phase = Phase.Done
                        }
                    }

                    override fun onReport(dx: Float, dy: Float, timeMs: Double) {
                        splitter[0].push(dx.toDouble(), dy.toDouble())
                    }

                    override fun onBatchEnd() {
                        val count = splitter[0].count
                        if (count != swipes) swipes = count
                    }

                    override fun onNonMouseInput() {
                        nonMouse = true
                    }
                }
            },
        )

        if (nonMouse) Note(stringResource(R.string.touchpad_hint), color = Lab.warn)

        val r = result
        val enough = r != null && r.swipes >= MIN_SWIPES
        ResultWell(
            label = stringResource(R.string.rot_result),
            value = if (enough) String.format(Locale.US, "%.1f°", r!!.correction) else "0°",
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (phase == Phase.Done && enough) {
                Note(stringResource(R.string.rot_margin, String.format(Locale.US, "%.1f", r!!.margin), r.swipes))
            }
            if (phase == Phase.Done && !enough) {
                Note(stringResource(R.string.rot_few, r?.swipes ?: 0, MIN_SWIPES), color = Lab.warn)
            }
        }

        Note(stringResource(R.string.rot_note))

        HowSection(
            title = stringResource(R.string.how_title),
            formula = stringResource(R.string.rot_formula),
            paragraphs = listOf(
                stringResource(R.string.rot_how_p1),
                stringResource(R.string.rot_how_p2),
                stringResource(R.string.rot_how_p3),
            ),
        )
    }
}
