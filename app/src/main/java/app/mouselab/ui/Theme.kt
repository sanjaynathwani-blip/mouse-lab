package app.mouselab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Lab {
    val ink = Color(0xFF0B0C0F)
    val surface1 = Color(0xFF14161C)
    val surface2 = Color(0xFF1E212A)
    val well = Color(0xFF101217)
    val line = Color(0xFF242833)
    val text1 = Color(0xFFF2F3F5)
    val text2 = Color(0xFFA9AEB8)
    val text3 = Color(0xFF727885)
    val accent = Color(0xFF7DD3C0)
    val warn = Color(0xFFF5B061)
}

@Composable
fun MouseLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lab.accent,
            onPrimary = Lab.ink,
            background = Lab.ink,
            onBackground = Lab.text1,
            surface = Lab.surface1,
            onSurface = Lab.text1,
        ),
        content = content,
    )
}
