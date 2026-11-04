package app.mouselab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mouselab.ui.Lab
import app.mouselab.ui.MouseLabTheme
import app.mouselab.ui.Note
import app.mouselab.ui.PollingScreen
import app.mouselab.ui.RotationScreen

const val SOURCE_URL = "https://github.com/sanjaynathwani-blip/mouse-lab"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MouseLabTheme { App() } }
    }
}

private enum class Tool { Polling, Rotation }

@Composable
private fun App() {
    var tool by rememberSaveable { mutableStateOf(Tool.Polling) }
    val context = LocalContext.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Lab.ink)
            .safeDrawingPadding(),
    ) {
        val wide = maxWidth >= 600.dp
        // The rotation box gets most of the window, so long swipes have room.
        val rotationPad = (maxHeight * 0.55f).coerceIn(280.dp, 560.dp)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxWidth()
                    .padding(horizontal = if (wide) 24.dp else 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.app_name), color = Lab.text1, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                    ToolChip(stringResource(R.string.tab_polling), tool == Tool.Polling) { tool = Tool.Polling }
                    ToolChip(stringResource(R.string.tab_rotation), tool == Tool.Rotation) { tool = Tool.Rotation }
                }
                when (tool) {
                    Tool.Polling -> PollingScreen(wide)
                    Tool.Rotation -> RotationScreen(rotationPad)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Note(stringResource(R.string.credit), modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
                    }) { Text(stringResource(R.string.source_link), color = Lab.accent) }
                }
            }
        }
    }
}

@Composable
private fun ToolChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Lab.surface1,
            labelColor = Lab.text2,
            selectedContainerColor = Lab.accent.copy(alpha = 0.16f),
            selectedLabelColor = Lab.accent,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Lab.line, selectedBorderColor = Lab.accent.copy(alpha = 0.5f)),
    )
}
