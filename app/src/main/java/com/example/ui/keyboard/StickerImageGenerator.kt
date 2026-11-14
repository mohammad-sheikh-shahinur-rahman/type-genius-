package com.example.ui.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream

data class StickerGradientPreset(
    val id: String,
    val name: String,
    val startColor: Int,
    val endColor: Int,
    val textColor: Int = Color.WHITE
)

enum class StickerFontStyle(val id: String, val label: String) {
    ATMA("atma", "আত্মা (হাতের লেখা)"),
    GALADA("galada", "গলাদা (ক্যালিগ্রাফি)"),
    BOLD("bold", "বোল্ড বাংলা")
}

object StickerImageGenerator {

    val gradientPresets: List<StickerGradientPreset> = listOf(
        StickerGradientPreset("tg_neon", "মেজেন্টা", 0xFFD500F9.toInt(), 0xFF651FFF.toInt()),
        StickerGradientPreset("sunset", "সোনালী সূর্য", 0xFFFF512F.toInt(), 0xFFF09819.toInt()),
        StickerGradientPreset("emerald", "সবুজ বাংলা", 0xFF009688.toInt(), 0xFF1B5E20.toInt()),
        StickerGradientPreset("ocean", "নীল আকাশ", 0xFF2196F3.toInt(), 0xFF00BCD4.toInt()),
        StickerGradientPreset("royal_dark", "ডার্ক রয়্যাল", 0xFF1A1A2E.toInt(), 0xFF4A148C.toInt()),
        StickerGradientPreset("rose", "গোলাপি মায়া", 0xFFEC407A.toInt(), 0xFFAD1457.toInt())
    )

    val quickStickersTextPresets: List<String> = listOf(
        "অস্থির হইছে বস!",
        "চা খাবেন নাকি?",
        "আসসালামু আলাইকুম",
        "ভালোবাসা অবিরাম",
        "কী খবর দোস্ত?",
        "আলহামদুলিল্লাহ"
    )

    fun renderStickerBitmap(
        context: Context,
        text: String,
        emoji: String,
        gradient: StickerGradientPreset,
        fontStyle: StickerFontStyle,
        sizePx: Int = 512
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer padding for sticker border glow
        val margin = sizePx * 0.06f
        val rect = RectF(margin, margin, sizePx - margin, sizePx - margin)
        val corner = sizePx * 0.14f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                gradient.startColor, gradient.endColor, Shader.TileMode.CLAMP
            )
            setShadowLayer(14f, 0f, 8f, 0x55000000)
        }
        canvas.drawRoundRect(rect, corner, corner, bgPaint)

        // White sticker outline stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.02f
            color = Color.argb(235, 255, 255, 255)
        }
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        // Subtle inner highlight
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(38, 255, 255, 255)
        }
        canvas.drawCircle(sizePx * 0.25f, sizePx * 0.22f, sizePx * 0.18f, glowPaint)

        // Top Emoji badge
        if (emoji.isNotBlank()) {
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = sizePx * 0.17f
            }
            canvas.drawText(emoji, sizePx / 2f, sizePx * 0.30f, emojiPaint)
        }

        // Load custom Bangla font safely (works in Robolectric too via runCatching)
        val typeface = when (fontStyle) {
            StickerFontStyle.ATMA -> runCatching { ResourcesCompat.getFont(context, R.font.atma) }.getOrNull()
            StickerFontStyle.GALADA -> runCatching { ResourcesCompat.getFont(context, R.font.galada) }.getOrNull()
            StickerFontStyle.BOLD -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val cleanText = text.trim().ifBlank { "বাংলাবিদ!" }.take(48)
        val textSizePx = when {
            cleanText.length <= 10 -> sizePx * 0.135f
            cleanText.length <= 20 -> sizePx * 0.11f
            cleanText.length <= 34 -> sizePx * 0.09f
            else -> sizePx * 0.075f
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gradient.textColor
            textSize = textSizePx
            this.typeface = typeface
            setShadowLayer(6f, 0f, 3f, 0x88000000)
        }

        val textWidth = (sizePx * 0.76f).toInt()
        @Suppress("DEPRECATION")
        val staticLayout = StaticLayout.Builder.obtain(cleanText, 0, cleanText.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f, 1.05f)
            .setMaxLines(3)
            .build()

        canvas.save()
        val textY = if (emoji.isNotBlank()) {
            sizePx * 0.56f - (staticLayout.height / 2f)
        } else {
            sizePx * 0.50f - (staticLayout.height / 2f)
        }
        canvas.translate((sizePx - textWidth) / 2f, textY.coerceAtLeast(sizePx * 0.32f))
        staticLayout.draw(canvas)
        canvas.restore()

        // Footer watermark pill
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(175, 255, 255, 255)
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.042f
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("বাংলাবিদ স্টিকার", sizePx / 2f, sizePx * 0.87f, footerPaint)

        return bitmap
    }

    fun generateStickerPngFile(
        context: Context,
        text: String,
        emoji: String,
        gradient: StickerGradientPreset,
        fontStyle: StickerFontStyle
    ): File {
        val dir = File(context.cacheDir, "stickers").apply { if (!exists()) mkdirs() }
        val file = File(dir, "tg_sticker_${System.currentTimeMillis()}.png")
        val bitmap = renderStickerBitmap(context, text, emoji, gradient, fontStyle)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return file
    }
}
