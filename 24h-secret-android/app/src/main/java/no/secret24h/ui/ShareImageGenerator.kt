package no.secret24h.ui

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import no.secret24h.data.MOOD_EMOJIS
import no.secret24h.data.Secret
import java.io.File

object ShareImageGenerator {

    private const val SIZE = 1080
    private const val CARD_MARGIN = 72f
    private const val CARD_PADDING = 52f
    private const val CORNER = 44f

    fun generate(context: Context, secret: Secret): File {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawCard(canvas, secret)
        drawBranding(canvas)

        val file = File(context.cacheDir, "24h_secret_share.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bitmap.recycle()
        return file
    }

    private fun drawBackground(canvas: Canvas) {
        val gradient = LinearGradient(
            0f, 0f, 0f, SIZE.toFloat(),
            intArrayOf(0xFF3A0F15.toInt(), 0xFF120608.toInt(), 0xFF07030A.toInt()),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPaint(Paint().apply { shader = gradient })
    }

    private fun drawCard(canvas: Canvas, secret: Secret) {
        val cardLeft   = CARD_MARGIN
        val cardTop    = SIZE * 0.13f
        val cardRight  = SIZE - CARD_MARGIN
        val cardBottom = SIZE * 0.76f
        val cardWidth  = cardRight - cardLeft

        // Glow behind card
        canvas.drawRoundRect(
            cardLeft - 8, cardTop - 8, cardRight + 8, cardBottom + 8,
            CORNER + 4, CORNER + 4,
            Paint().apply {
                color = 0x33FF7A4D.toInt()
                maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL)
            },
        )

        // Card background
        canvas.drawRoundRect(
            cardLeft, cardTop, cardRight, cardBottom, CORNER, CORNER,
            Paint().apply { color = 0xFF1C0A10.toInt() },
        )

        // Card border
        canvas.drawRoundRect(
            cardLeft, cardTop, cardRight, cardBottom, CORNER, CORNER,
            Paint().apply {
                color = 0x28FFFFFF.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 2f
            },
        )

        // Lock emoji top-left
        val lockPaint = TextPaint().apply {
            textSize = 52f
            isAntiAlias = true
        }
        canvas.drawText("🔒", cardLeft + CARD_PADDING, cardTop + CARD_PADDING + 48, lockPaint)

        // Secret text
        val secretTextSize = when {
            secret.text.length > 140 -> 42f
            secret.text.length > 80  -> 50f
            else                     -> 58f
        }
        val secretPaint = TextPaint().apply {
            color = 0xFFEDE8D5.toInt()
            textSize = secretTextSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val textWidth = (cardWidth - CARD_PADDING * 2).toInt()
        val secretLayout = StaticLayout.Builder
            .obtain(secret.text, 0, secret.text.length, secretPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f, 1.25f)
            .setMaxLines(7)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        // Vertically centre the text block in the card
        val cardHeight    = cardBottom - cardTop
        val textBlockH    = secretLayout.height.toFloat()
        val textTopOffset = (cardHeight - textBlockH) / 2f - 20f
        val textY         = cardTop + textTopOffset.coerceAtLeast(CARD_PADDING + 80f)

        canvas.save()
        canvas.translate(cardLeft + CARD_PADDING, textY)
        secretLayout.draw(canvas)
        canvas.restore()

        // Mood + countdown row at bottom of card
        val emoji = MOOD_EMOJIS[secret.mood] ?: "💭"
        val metaPaint = TextPaint().apply {
            color = 0xAAFF7A4D.toInt()
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText(
            "$emoji  ${secret.mood}  ·  disappears in 24h ⏳",
            cardLeft + CARD_PADDING,
            cardBottom - CARD_PADDING,
            metaPaint,
        )
    }

    private fun drawBranding(canvas: Canvas) {
        // App name
        val titlePaint = TextPaint().apply {
            color = 0xFFFF7A4D.toInt()
            textSize = 64f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("24h Secret", SIZE / 2f, SIZE * 0.855f, titlePaint)

        // Tagline
        val tagPaint = TextPaint().apply {
            color = 0x66FFFFFF.toInt()
            textSize = 30f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("A world of secrets. Gone in 24h.", SIZE / 2f, SIZE * 0.91f, tagPaint)

        // CTA
        val ctaPaint = TextPaint().apply {
            color = 0x44FFFFFF.toInt()
            textSize = 26f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Share your own secret at 24h-secret.no", SIZE / 2f, SIZE * 0.955f, ctaPaint)
    }
}
