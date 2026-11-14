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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class GifPresetItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val emoji: String,
    val startColor: Int,
    val endColor: Int,
    val accentColor: Int,
    val keywords: List<String>
)

object AnimatedGifBuilder {

    val categories: List<String> = listOf("সব", "অনুভূতি", "সম্ভাষণ", "উৎসব ও দোয়া", "আড্ডা")

    val presets: List<GifPresetItem> = listOf(
        GifPresetItem("osthir", "অস্থির হইছে!", "দারুণ কাজ 🔥", "অনুভূতি", "🔥", 0xFFFF512F.toInt(), 0xFFDD2476.toInt(), 0xFFFFD54F.toInt(), listOf("osthir", "fire", "awesome", "অস্থির", "দারুণ")),
        GifPresetItem("love", "ভালোবাসা ❤️", "অবিরাম মায়া", "অনুভূতি", "❤️", 0xFFEC008C.toInt(), 0xFFFC6767.toInt(), 0xFFFFEBEE.toInt(), listOf("love", "heart", "ভালোবাসা", "প্রেম", "valobasha")),
        GifPresetItem("haha", "হাসতে শেষ 😂", "হাহাহা রিয়েল!", "অনুভূতি", "😂", 0xFFF7971E.toInt(), 0xFFFFD200.toInt(), 0xFF3E2723.toInt(), listOf("haha", "laugh", "funny", "হাসি", "মজা")),
        GifPresetItem("salam", "আসসালামু আলাইকুম", "শান্তি বর্ষিত হোক 🤲", "সম্ভাষণ", "🤲", 0xFF11998E.toInt(), 0xFF38EF7D.toInt(), 0xFFFFFFFF.toInt(), listOf("salam", "assalamu", "সালাম", "সম্ভাষণ")),
        GifPresetItem("shokal", "শুভ সকাল ☀️", "সুন্দর কাটুক দিন", "সম্ভাষণ", "☀️", 0xFFFF8008.toInt(), 0xFFFFC837.toInt(), 0xFF4E342E.toInt(), listOf("morning", "shokal", "সকাল", "শুভ সকাল")),
        GifPresetItem("ratri", "শুভ রাত্রি 🌙", "সুখনিদ্রা ✨", "সম্ভাষণ", "🌙", 0xFF141E30.toInt(), 0xFF243B55.toInt(), 0xFFFFE082.toInt(), listOf("night", "goodnight", "রাত", "শুভ রাত্রি")),
        GifPresetItem("eid", "ঈদ মোবারক 🌙", "আনন্দময় ঈদ ✨", "উৎসব ও দোয়া", "✨", 0xFF4A00E0.toInt(), 0xFF8E2DE2.toInt(), 0xFFFFD700.toInt(), listOf("eid", "mubarak", "ঈদ", "উৎসব")),
        GifPresetItem("inshallah", "ইনশাআল্লাহ 🤍", "আল্লাহ ভরসা", "উৎসব ও দোয়া", "🤍", 0xFF00B4DB.toInt(), 0xFF0083B0.toInt(), 0xFFE0F7FA.toInt(), listOf("inshallah", "dua", "ইনশাআল্লাহ", "দোয়া")),
        GifPresetItem("thanks", "অনেক ধন্যবাদ 🙏", "কৃতজ্ঞতা রইলো", "অনুভূতি", "🙏", 0xFF667EEA.toInt(), 0xFF764BA2.toInt(), 0xFFFFFFFF.toInt(), listOf("thanks", "thank", "ধন্যবাদ", "dhonnobad")),
        GifPresetItem("salute", "স্যালুট বস! 🫡", "একদম ঠিক কথা", "অনুভূতি", "🫡", 0xFF2193B0.toInt(), 0xFF6DD5ED.toInt(), 0xFF0D47A1.toInt(), listOf("salute", "boss", "স্যালুট", "ঠিক")),
        GifPresetItem("chai", "চলো চা খাই ☕", "আড্ডার সময়", "আড্ডা", "☕", 0xFF603813.toInt(), 0xFFB29F94.toInt(), 0xFFFFF8E1.toInt(), listOf("tea", "chai", "cha", "চা", "আড্ডা")),
        GifPresetItem("chill", "ব্যাপার না, চিল 😎", "টেনশন ফ্রি", "আড্ডা", "😎", 0xFF0F2027.toInt(), 0xFF2C5364.toInt(), 0xFF64FFDA.toInt(), listOf("chill", "cool", "চিল", "কুল"))
    )

    fun getOrCreatePresetGif(context: Context, item: GifPresetItem): File {
        val dir = File(context.cacheDir, "gifs").apply { if (!exists()) mkdirs() }
        val file = File(dir, "tg_gif_${item.id}_v1.gif")
        if (file.exists() && file.length() > 512L) return file

        val frames = renderAnimatedFrames(
            mainText = item.title,
            subText = item.subtitle,
            emoji = item.emoji,
            startColor = item.startColor,
            endColor = item.endColor,
            accentColor = item.accentColor
        )
        encodeGif(frames, delayCs = 14, file = file)
        return file
    }

    fun createCustomTextGif(
        context: Context,
        text: String,
        emoji: String = "✨",
        startColor: Int = 0xFF7C4DFF.toInt(),
        endColor: Int = 0xFFE91E63.toInt()
    ): File {
        val dir = File(context.cacheDir, "gifs").apply { if (!exists()) mkdirs() }
        val safeHash = "${text}_${emoji}_$startColor".hashCode().toUInt().toString(16)
        val file = File(dir, "tg_custom_gif_$safeHash.gif")
        if (file.exists() && file.length() > 512L) return file

        val cleanText = text.trim().ifBlank { "বাংলাবিদ!" }.take(36)
        val frames = renderAnimatedFrames(
            mainText = cleanText,
            subText = "TypeGenius বাংলাবিদ",
            emoji = emoji,
            startColor = startColor,
            endColor = endColor,
            accentColor = Color.WHITE
        )
        encodeGif(frames, delayCs = 14, file = file)
        return file
    }

    fun renderAnimatedFrames(
        mainText: String,
        subText: String,
        emoji: String,
        startColor: Int,
        endColor: Int,
        accentColor: Int,
        width: Int = 240,
        height: Int = 136,
        frameCount: Int = 8
    ): List<Bitmap> {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 0f, 2f, 0x77000000)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 15f
        }
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 30f
        }

        return (0 until frameCount).map { idx ->
            val t = idx.toFloat() / frameCount.toFloat()
            val angle = t * 2f * PI.toFloat()
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Animated diagonal gradient shift
            val shift = sin(angle) * 28f
            bgPaint.shader = LinearGradient(
                0f, shift, width.toFloat(), height.toFloat() - shift,
                startColor, endColor, Shader.TileMode.CLAMP
            )
            val outerRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(outerRect, 20f, 20f, bgPaint)

            // Pulsing glassmorphic inner border
            cardPaint.style = Paint.Style.STROKE
            cardPaint.strokeWidth = 3f
            val alphaBorder = (130 + (sin(angle) * 90)).toInt().coerceIn(60, 240)
            cardPaint.color = Color.argb(alphaBorder, 255, 255, 255)
            canvas.drawRoundRect(RectF(6f, 6f, width - 6f, height - 6f), 16f, 16f, cardPaint)

            // Orbiting sparkle dots
            for (s in 0 until 6) {
                val p = angle + s * (2f * PI.toFloat() / 6f)
                val sx = (width * 0.5f) + cos(p) * (width * 0.39f)
                val sy = (height * 0.5f) + sin(p * 1.5f) * (height * 0.36f)
                val radius = 2.5f + 2f * ((sin(p * 2f) + 1f) * 0.5f)
                sparklePaint.color = Color.argb(180, 255, 255, 255)
                canvas.drawCircle(sx, sy, radius, sparklePaint)
            }

            // Bouncing emoji
            val bounceY = 38f + sin(angle) * 5f
            canvas.drawText(emoji, width / 2f, bounceY, emojiPaint)

            // Pulsing title text
            val cleanTitle = mainText.trim().ifBlank { "বাংলাবিদ!" }
            val baseSize = when {
                cleanTitle.length > 22 -> 16f
                cleanTitle.length > 14 -> 20f
                else -> 24f
            }
            titlePaint.textSize = baseSize + (sin(angle) * 1.4f)
            canvas.drawText(cleanTitle, width / 2f, height * 0.62f, titlePaint)

            // Subtitle
            if (subText.isNotBlank()) {
                canvas.drawText(subText, width / 2f, height * 0.84f, subPaint)
            }
            bmp
        }
    }

    /**
     * Encodes a list of ARGB Bitmaps into a valid looping GIF89a byte array and writes to [file].
     */
    fun encodeGif(frames: List<Bitmap>, delayCs: Int, file: File): ByteArray {
        val bytes = encodeGifBytes(frames, delayCs)
        FileOutputStream(file).use { it.write(bytes) }
        return bytes
    }

    fun encodeGifBytes(frames: List<Bitmap>, delayCs: Int = 14): ByteArray {
        if (frames.isEmpty()) return ByteArray(0)
        val width = frames.first().width
        val height = frames.first().height
        val out = ByteArrayOutputStream(32 * 1024)

        // 1. GIF89a Header
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))

        // 2. Logical Screen Descriptor (no Global Color Table — each frame carries an optimal local 64-color table)
        writeShortLe(out, width)
        writeShortLe(out, height)
        out.write(0x70) // 0 (no GCT), color res 7, not sorted, 0 size
        out.write(0)    // bg color idx
        out.write(0)    // pixel aspect ratio

        // 3. Netscape 2.0 Application Extension (infinite loop)
        out.write(0x21)
        out.write(0xFF)
        out.write(11)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3)
        out.write(1)
        writeShortLe(out, 0) // loop forever
        out.write(0)

        // 4. Write each frame with uniform 6-bit (64-color) quantized Local Color Table -> crisp & ultra fast
        val palette = buildWeb64Palette()
        val pixels = IntArray(width * height)

        for (frame in frames) {
            val scaled = if (frame.width != width || frame.height != height) {
                Bitmap.createScaledBitmap(frame, width, height, true)
            } else frame
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)

            // Graphic Control Extension
            out.write(0x21)
            out.write(0xF9)
            out.write(4)
            out.write(0x04) // disposal = do not dispose
            writeShortLe(out, delayCs.coerceAtLeast(4))
            out.write(0)    // transparent index (unused)
            out.write(0)

            // Image Descriptor (with 64-color Local Color Table: flag 0x85 -> 2^(5+1) = 64 colors)
            out.write(0x2C)
            writeShortLe(out, 0)
            writeShortLe(out, 0)
            writeShortLe(out, width)
            writeShortLe(out, height)
            out.write(0x85) // local color table present, size = 5 (64 entries)

            // Write 64 RGB triplets
            out.write(palette)

            // Map ARGB pixels to 6-bit quantized color indices (2 bits R, 2 bits G, 2 bits B)
            val indices = ByteArray(pixels.size)
            for (i in pixels.indices) {
                val p = pixels[i]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                val idx = ((r ushr 6) shl 4) or ((g ushr 6) shl 2) or (b ushr 6)
                indices[i] = idx.toByte()
            }

            // LZW compress image pixels
            lzwEncode(indices, minCodeSize = 6, out = out)
        }

        // GIF Trailer
        out.write(0x3B)
        return out.toByteArray()
    }

    private fun buildWeb64Palette(): ByteArray {
        val bytes = ByteArray(64 * 3)
        val levels = intArrayOf(0, 85, 170, 255)
        var pos = 0
        for (r in 0..3) {
            for (g in 0..3) {
                for (b in 0..3) {
                    bytes[pos++] = levels[r].toByte()
                    bytes[pos++] = levels[g].toByte()
                    bytes[pos++] = levels[b].toByte()
                }
            }
        }
        return bytes
    }

    private fun writeShortLe(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    private fun lzwEncode(indices: ByteArray, minCodeSize: Int, out: OutputStream) {
        out.write(minCodeSize)
        val clearCode = 1 shl minCodeSize
        val eofCode = clearCode + 1

        val blockBuf = ByteArray(256)
        var blockLen = 0
        var curBitBuf = 0
        var curBits = 0

        fun flushBlock() {
            if (blockLen > 0) {
                out.write(blockLen)
                out.write(blockBuf, 0, blockLen)
                blockLen = 0
            }
        }

        fun writeBits(code: Int, codeSize: Int) {
            curBitBuf = curBitBuf or (code shl curBits)
            curBits += codeSize
            while (curBits >= 8) {
                blockBuf[blockLen++] = (curBitBuf and 0xFF).toByte()
                if (blockLen == 255) flushBlock()
                curBitBuf = curBitBuf ushr 8
                curBits -= 8
            }
        }

        var codeSize = minCodeSize + 1
        var nextCode = eofCode + 1
        val table = HashMap<Int, Int>(4096)

        writeBits(clearCode, codeSize)
        var prefix = indices[0].toInt() and 0xFF

        for (i in 1 until indices.size) {
            val k = indices[i].toInt() and 0xFF
            val key = (prefix shl 8) or k
            val existing = table[key]
            if (existing != null) {
                prefix = existing
            } else {
                writeBits(prefix, codeSize)
                if (nextCode < 4096) {
                    table[key] = nextCode
                    if (nextCode == (1 shl codeSize) && codeSize < 12) {
                        codeSize++
                    }
                    nextCode++
                } else {
                    writeBits(clearCode, codeSize)
                    table.clear()
                    codeSize = minCodeSize + 1
                    nextCode = eofCode + 1
                }
                prefix = k
            }
        }

        writeBits(prefix, codeSize)
        writeBits(eofCode, codeSize)
        if (curBits > 0) {
            blockBuf[blockLen++] = (curBitBuf and 0xFF).toByte()
        }
        flushBlock()
        out.write(0) // Block Terminator
    }
}
