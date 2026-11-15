package com.bunde.learning.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import com.caverock.androidsvg.SVG
import java.io.File
import java.security.MessageDigest

/**
 * Memory-safe SVG and bitmap rendering + disk caching utility
 * tailored for low-RAM devices (2GB RAM).
 */
object VisualRenderer {

    private const val MAX_RENDER_WIDTH_PX = 960
    private const val MAX_RENDER_HEIGHT_PX = 760

    fun sanitizeSvg(raw: String): String {
        var s = raw.trim()
        // Remove unsafe or unsupported tags
        s = s.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<foreignObject[\\s\\S]*?</foreignObject>", RegexOption.IGNORE_CASE), "")
        // Escape stray ampersands not already part of XML entities
        s = s.replace(Regex("&(?!(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)"), "&amp;")

        // Ensure SVG namespace exists
        if (s.contains("<svg", ignoreCase = true) && !s.contains("xmlns=", ignoreCase = true)) {
            s = s.replaceFirst(Regex("<svg", RegexOption.IGNORE_CASE), "<svg xmlns=\"http://www.w3.org/2000/svg\"")
        }
        return s
    }

    fun renderSvgToBitmap(rawSvg: String, targetWidthPx: Int = MAX_RENDER_WIDTH_PX): Bitmap? {
        return try {
            val sanitized = sanitizeSvg(rawSvg)
            if (!sanitized.contains("<svg", ignoreCase = true)) return null

            val svg = SVG.getFromString(sanitized)
            val vb = svg.documentViewBox
            val docW = when {
                vb != null && vb.width() > 0f -> vb.width()
                svg.documentWidth > 0f -> svg.documentWidth
                else -> 420f
            }
            val docH = when {
                vb != null && vb.height() > 0f -> vb.height()
                svg.documentHeight > 0f -> svg.documentHeight
                else -> 260f
            }

            if (vb == null) {
                svg.setDocumentViewBox(0f, 0f, docW, docH)
            }

            val aspect = (docH / docW).coerceIn(0.35f, 1.5f)
            val width = targetWidthPx.coerceIn(320, MAX_RENDER_WIDTH_PX)
            val height = (width * aspect).toInt().coerceIn(180, MAX_RENDER_HEIGHT_PX)

            svg.documentWidth = width.toFloat()
            svg.documentHeight = height.toFloat()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            svg.renderToCanvas(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDownsampledBitmap(bytes: ByteArray, maxDim: Int = 1024): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > maxDim || (bounds.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Exception) {
            null
        }
    }

    fun loadCachedImageBytes(context: Context, key: String): ByteArray? {
        return try {
            val file = cacheFile(context, key, "img")
            if (file.exists() && file.length() > 0) file.readBytes() else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveCachedImageBytes(context: Context, key: String, bytes: ByteArray) {
        try {
            val file = cacheFile(context, key, "img")
            file.writeBytes(bytes)
        } catch (_: Exception) {}
    }

    fun loadCachedSvg(context: Context, key: String): String? {
        return try {
            val file = cacheFile(context, key, "svg")
            if (file.exists() && file.length() > 0) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveCachedSvg(context: Context, key: String, svg: String) {
        try {
            val file = cacheFile(context, key, "svg")
            file.writeText(svg)
        } catch (_: Exception) {}
    }

    private fun cacheFile(context: Context, key: String, ext: String): File {
        val dir = File(context.cacheDir, "bunde_visuals")
        if (!dir.exists()) dir.mkdirs()
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        val hex = digest.take(12).joinToString("") { "%02x".format(it) }
        return File(dir, "$hex.$ext")
    }
}
