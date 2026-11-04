package app.mouselab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

val PadShape = RoundedCornerShape(18.dp)
val WellShape = RoundedCornerShape(16.dp)

/** The measuring box: a [CapturePadView] with a status line over it. */
@Composable
fun CapturePad(
    height: Dp,
    active: Boolean,
    status: String,
    footer: String?,
    statusOnTop: Boolean = active,
    create: (CapturePadView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(PadShape)
            .background(Lab.surface1)
            .border(if (active) 2.dp else 1.dp, if (active) Lab.accent.copy(alpha = 0.45f) else Lab.line, PadShape),
    ) {
        AndroidView(
            factory = { context ->
                CapturePadView(context).apply {
                    trailColor = Lab.accent.toArgb()
                    create(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            status,
            color = if (active) Lab.text3 else Lab.text2,
            fontSize = if (statusOnTop) 13.sp else 15.sp,
            fontFamily = if (statusOnTop) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(if (statusOnTop) Alignment.TopCenter else Alignment.Center)
                .padding(16.dp),
        )
        if (footer != null) {
            Text(
                footer,
                color = Lab.text3,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

/** A big monospaced reading in a recessed well. */
@Composable
fun ResultWell(label: String, value: String, unit: String? = null, modifier: Modifier = Modifier, extra: @Composable () -> Unit = {}) {
    Column(
        modifier
            .clip(WellShape)
            .background(Lab.well)
            .border(1.dp, Lab.line, WellShape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = Lab.text2, fontSize = 14.sp)
        Text(
            buildAnnotatedString {
                append(value)
                if (unit != null) {
                    withStyle(SpanStyle(color = Lab.text3, fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Default)) {
                        append(" $unit")
                    }
                }
            },
            color = Lab.text1,
            fontSize = 40.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
        extra()
    }
}

@Composable
fun Note(text: String, color: androidx.compose.ui.graphics.Color = Lab.text3, modifier: Modifier = Modifier) {
    Text(text, color = color, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = modifier)
}

@Composable
fun HowSection(paragraphs: List<String>, formula: String? = null, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(color = Lab.line)
        Text(title, color = Lab.text1, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
        paragraphs.forEachIndexed { index, p ->
            Text(p, color = Lab.text2, fontSize = 15.sp, lineHeight = 24.sp)
            if (index == 0 && formula != null) {
                Text(
                    formula,
                    color = Lab.text1,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Lab.well)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
fun Header(title: String, lead: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Lab.text1, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
        Text(lead, color = Lab.text2, fontSize = 16.sp, lineHeight = 24.sp)
    }
}
