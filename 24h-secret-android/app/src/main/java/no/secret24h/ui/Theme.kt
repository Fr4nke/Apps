package no.secret24h.ui

import android.graphics.BlurMaskFilter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.secret24h.R

// ── Palette ──────────────────────────────────────────────────────────────────
val DuskBg        = Color(0xFF0C0A1A)
val DuskSurface   = Color(0x0AFFFFFF)   // rgba(255,255,255,0.04)
val DuskBorder    = Color(0x14FFFFFF)   // rgba(255,255,255,0.08)
val DuskAccent    = Color(0xFFB69DFF)
val DuskGlow      = Color(0xFF7C5CFC)
val DuskText      = Color(0xFFF0EEFF)
val DuskMuted     = Color(0xFF8B88A4)
val DuskCardBg    = Color(0x0DFFFFFF)   // rgba(255,255,255,0.05)

// Legacy aliases kept so other files compile without change
val VioletLight   = DuskAccent
val Zinc600       = DuskMuted
val Zinc700       = Color(0xFF3F3A55)
val Zinc800       = Color(0xFF1E1B2E)
val Zinc900       = Color(0xFF13111F)

// ── Emotion colors ────────────────────────────────────────────────────────────
data class EmotionColors(val bg: Color, val fg: Color, val glow: Color)

val EMOTION_COLORS = mapOf(
    "lettelse" to EmotionColors(Color(0xFF0D1F42), Color(0xFF5B8FFF), Color(0xFF3D6AFF)),
    "skam"     to EmotionColors(Color(0xFF3D0A1A), Color(0xFFFF6B8A), Color(0xFFFF3D6B)),
    "stolthet" to EmotionColors(Color(0xFF3D2A00), Color(0xFFFFD166), Color(0xFFFFB800)),
    "anger"    to EmotionColors(Color(0xFF3D1500), Color(0xFFFF784F), Color(0xFFFF4D1A)),
    "annet"    to EmotionColors(Color(0xFF18162A), Color(0xFF8B88A4), Color(0xFF5C5980)),
)

// ── Google Fonts ──────────────────────────────────────────────────────────────
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

val InstrumentSerif = FontFamily(
    Font(googleFont = GoogleFont("Instrument Serif"), fontProvider = fontProvider, weight = FontWeight.Normal),
)

val GeistFamily = FontFamily(
    Font(googleFont = GoogleFont("Geist"), fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Geist"), fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Geist"), fontProvider = fontProvider, weight = FontWeight.Bold),
)

// ── Theme ─────────────────────────────────────────────────────────────────────
private val colorScheme = darkColorScheme(
    background        = DuskBg,
    surface           = Zinc900,
    surfaceVariant    = Zinc800,
    primary           = DuskAccent,
    onPrimary         = Color.White,
    onBackground      = DuskText,
    onSurface         = DuskText,
    onSurfaceVariant  = DuskMuted,
    error             = Color(0xFFFF6B8A),
)

private val typography = Typography(
    displayLarge  = TextStyle(fontFamily = InstrumentSerif, fontSize = 36.sp),
    headlineMedium = TextStyle(fontFamily = InstrumentSerif, fontSize = 24.sp),
    bodyLarge     = TextStyle(fontFamily = GeistFamily, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontFamily = GeistFamily, fontSize = 13.sp),
    labelSmall    = TextStyle(fontFamily = GeistFamily, fontSize = 11.sp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}

// ── Glow modifier ─────────────────────────────────────────────────────────────
fun androidx.compose.ui.Modifier.glowBehind(
    color: Color,
    radius: Dp = 20.dp,
    cornerRadius: Dp = 16.dp,
    alpha: Float = 0.35f,
) = drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.maskFilter = BlurMaskFilter(radius.toPx(), BlurMaskFilter.Blur.NORMAL)
        fp.color = color.copy(alpha = alpha).toArgb()
        canvas.drawRoundRect(0f, 0f, size.width, size.height, cornerRadius.toPx(), cornerRadius.toPx(), paint)
    }
}
