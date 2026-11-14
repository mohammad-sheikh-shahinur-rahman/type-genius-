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
import com.example.data.CustomFontManager
import com.example.data.dictionary.FancyFontStyle
import com.example.data.dictionary.UnicodeTextStyler
import java.io.File
import java.io.FileOutputStream

enum class HandwritingPaperTheme(
    val id: String,
    val label: String,
    val bgStart: Int,
    val bgEnd: Int,
    val inkColor: Int,
    val hasRuledLines: Boolean = false
) {
    NOTEBOOK(
        id = "notebook",
        label = "খাতা",
        bgStart = 0xFFFFFDF5.toInt(),
        bgEnd = 0xFFFFF8E1.toInt(),
        inkColor = 0xFF1A237E.toInt(),
        hasRuledLines = true
    ),
    CLEAN_WHITE(
        id = "white",
        label = "সাদা",
        bgStart = 0xFFFFFFFF.toInt(),
        bgEnd = 0xFFF7F8FC.toInt(),
        inkColor = 0xFF14161F.toInt()
    ),
    DARK_INK(
        id = "dark",
        label = "ডার্ক",
        bgStart = 0xFF161925.toInt(),
        bgEnd = 0xFF1F2338.toInt(),
        inkColor = 0xFFFDF8EC.toInt()
    ),
    ROYAL_GRADIENT(
        id = "gradient",
        label = "রঙিন",
        bgStart = 0xFF6A1B9A.toInt(),
        bgEnd = 0xFFD81B60.toInt(),
        inkColor = 0xFFFFFFFF.toInt()
    )
}

object HandwritingOutputGenerator {

    fun renderHandwritingBitmap(
        context: Context,
        rawText: String,
        style: FancyFontStyle,
        customFontPath: String,
        customFontName: String = "",
        paperTheme: HandwritingPaperTheme = HandwritingPaperTheme.NOTEBOOK,
        widthPx: Int = 1080
    ): Bitmap {
        val displayText = if (style.isHandwritingFont) {
            rawText.trim().ifBlank { "আমার হাতের লেখা" }
        } else {
            UnicodeTextStyler.styleText(rawText.trim().ifBlank { "TypeGenius বাংলা" }, style)
        }.take(240)

        val typeface = CustomFontManager.getTypefaceForFancyStyle(context, style, customFontPath)
        val textSizePx = when {
            displayText.length <= 24 -> 72f
            displayText.length <= 60 -> 58f
            displayText.length <= 120 -> 48f
            else -> 40f
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paperTheme.inkColor
            textSize = textSizePx
            this.typeface = typeface
        }

        val horizontalPadding = 88
        val contentWidth = (widthPx - horizontalPadding * 2).coerceAtLeast(300)
        val staticLayout = StaticLayout.Builder
            .obtain(displayText, 0, displayText.length, textPaint, contentWidth)
            .setAlignment(
                if (displayText.length <= 36 && !displayText.contains('\n')) {
                    Layout.Alignment.ALIGN_CENTER
                } else {
                    Layout.Alignment.ALIGN_NORMAL
                }
            )
            .setLineSpacing(14f, 1.18f)
            .setMaxLines(10)
            .build()

        val verticalPadding = 92
        val footerHeight = 52
        val heightPx = (staticLayout.height + verticalPadding * 2 + footerHeight).coerceIn(360, 1280)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cardRect = RectF(16f, 16f, widthPx - 16f, heightPx - 16f)
        val corner = 44f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                paperTheme.bgStart, paperTheme.bgEnd, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        // Subtle border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 3f
            color = if (paperTheme == HandwritingPaperTheme.DARK_INK || paperTheme == HandwritingPaperTheme.ROYAL_GRADIENT) {
                Color.argb(65, 255, 255, 255)
            } else {
                Color.argb(40, 30, 40, 80)
            }
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        // Optional ruled notebook lines for authentic handwritten paper look
        if (paperTheme.hasRuledLines) {
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 2f
                color = 0x3364B5F6
            }
            var y = 110f
            while (y < heightPx - 70f) {
                canvas.drawLine(36f, y, widthPx - 36f, y, linePaint)
                y += 68f
            }
            val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 2.5f
                color = 0x44EF5350
            }
            canvas.drawLine(72f, 28f, 72f, heightPx - 28f, marginPaint)
        }

        // Draw handwritten / styled body text
        canvas.save()
        val textTop = ((heightPx - footerHeight) - staticLayout.height) / 2f
        canvas.translate(horizontalPadding.toFloat(), textTop.coerceAtLeast(56f))
        staticLayout.draw(canvas)
        canvas.restore()

        // Clean font attribution pill at bottom right
        val fontLabel = when (style) {
            FancyFontStyle.HANDWRITING_CUSTOM -> if (customFontName.isNotBlank()) "✍️ $customFontName" else "✍️ আমার হাতের লেখা"
            FancyFontStyle.HANDWRITING_ATMA -> "✍️ হাতের লেখা • Atma"
            FancyFontStyle.HANDWRITING_GALADA -> "🖋️ ক্যালিগ্রাফি • Galada"
            else -> "✨ ${style.displayName}"
        }
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 23f
            textAlign = Paint.Align.RIGHT
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.argb(
                135,
                Color.red(paperTheme.inkColor),
                Color.green(paperTheme.inkColor),
                Color.blue(paperTheme.inkColor)
            )
        }
        canvas.drawText(fontLabel, widthPx - 48f, heightPx - 36f, tagPaint)

        return bitmap
    }

    fun generateHandwritingPngFile(
        context: Context,
        rawText: String,
        style: FancyFontStyle,
        customFontPath: String,
        customFontName: String = "",
        paperTheme: HandwritingPaperTheme = HandwritingPaperTheme.NOTEBOOK
    ): File {
        val dir = File(context.cacheDir, "stickers").apply { if (!exists()) mkdirs() }
        val file = File(dir, "tg_hw_${System.currentTimeMillis()}.png")
        val bmp = renderHandwritingBitmap(
            context = context,
            rawText = rawText,
            style = style,
            customFontPath = customFontPath,
            customFontName = customFontName,
            paperTheme = paperTheme
        )
        FileOutputStream(file).use { fos ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return file
    }
}
