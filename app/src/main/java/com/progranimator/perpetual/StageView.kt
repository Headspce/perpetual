package com.progranimator.perpetual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import java.io.File

/**
 * Draws the whole screen: cartridge background, Wind Waker style tabs,
 * item grid, hint text, and THE progress bar.
 *
 * The bar itself is drawn by [drawPixelBar], whose geometry and style
 * constants are frozen — cartridges may only change its label text,
 * live progress value and accent color, never its graphics. The fill
 * is genuinely live: it eases toward the manifest's progress value,
 * which Wren updates as real work completes.
 */
class StageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var cartridge: Cartridge? = null
        set(value) {
            field = value
            loadBitmaps()
            // Don't reset the bar — ease from wherever it is toward the
            // new live progress target. Wren updates progress in the
            // manifest as real work completes.
            targetProgress = value?.progress ?: 0f
            invalidate()
        }

    var pixelFont: Typeface? = null
        set(value) { field = value; invalidate() }

    /** Directory holding the current cartridge's PNG assets. */
    var assetDir: File? = null

    private val icons = mutableMapOf<String, Bitmap>()
    private var bgBitmap: Bitmap? = null
    private val crisp = Paint().apply { isFilterBitmap = false }
    private val textPaint = Paint().apply { isAntiAlias = false }
    private val fillPaint = Paint().apply { isAntiAlias = false }

    // ---- Live progress: the bar eases toward the manifest's progress
    // value, which Wren updates as real work completes. No fake loop —
    // when the target moves, the fill glides to it (exponential ease,
    // ~1.5s to settle). The bar's graphics stay frozen; only the live
    // fill value moves.
    private var animProgress = 0f
    private var targetProgress = 0f
    private val animStep = object : Runnable {
        override fun run() {
            val c = cartridge
            if (c != null) targetProgress = c.progress.coerceIn(0f, 1f)
            // Exponential ease toward target: fast at first, gentle landing.
            val diff = targetProgress - animProgress
            animProgress = if (kotlin.math.abs(diff) < 0.001f) {
                targetProgress
            } else {
                (animProgress + diff * 0.12f).coerceIn(0f, 1f)
            }
            invalidate()
            postDelayed(this, 50) // 20 fps
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(animStep)
        post(animStep)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animStep)
        super.onDetachedFromWindow()
    }

    private fun loadBitmaps() {
        icons.clear()
        bgBitmap = null
        val c = cartridge ?: return
        val dir = assetDir ?: return
        fun load(name: String): Bitmap? = try {
            BitmapFactory.decodeFile(File(dir, name).absolutePath)
        } catch (_: Exception) { null }
        c.backgroundImage?.let { bgBitmap = load(it) }
        c.grid.forEach { item -> load(item.icon)?.let { icons[item.icon] = it } }
    }

    override fun onDraw(canvas: Canvas) {
        val c = cartridge ?: return
        val tf = pixelFont ?: Typeface.MONOSPACE
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val u = w / 120f // one "pixel unit"

        // ---- background ----
        canvas.drawColor(c.backgroundColor)
        bgBitmap?.let { bmp ->
            val s = maxOf(w / bmp.width, h / bmp.height)
            val dw = bmp.width * s
            val dh = bmp.height * s
            canvas.drawBitmap(
                bmp, null,
                RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2),
                crisp
            )
        }

        // ---- tabs (Wind Waker style) ----
        val tabPalette = intArrayOf(
            0xFF3FA7FF.toInt(), // blue
            0xFFFFB13D.toInt(), // orange
            0xFF7DDB6A.toInt(), // green
            0xFFC77DFF.toInt(), // purple
        )
        val nTabs = c.tabs.size.coerceAtLeast(1)
        val tabW = w / nTabs
        val tabH = 11 * u
        val tabY = 3 * u
        c.tabs.forEachIndexed { i, label ->
            val x = i * tabW
            val active = i == c.activeTab
            val col = tabPalette[i % tabPalette.size]
            // chunky black outline
            fillPaint.color = Color.BLACK
            canvas.drawRect(x + u, tabY, x + tabW - u, tabY + tabH, fillPaint)
            // face
            fillPaint.color = if (active) col else dim(col)
            canvas.drawRect(
                x + u + 0.7f * u, tabY + 0.7f * u,
                x + tabW - u - 0.7f * u, tabY + tabH, fillPaint
            )
            // top highlight strip
            fillPaint.color = if (active) lighten(col) else dim(lighten(col))
            canvas.drawRect(
                x + u + 0.7f * u, tabY + 0.7f * u,
                x + tabW - u - 0.7f * u, tabY + 2.2f * u, fillPaint
            )
            drawCenteredText(
                canvas, label, x + tabW / 2, tabY + tabH / 2 + u,
                4.6f * u, tf, Color.BLACK
            )
        }

        // ---- item grid ----
        val cols = c.columns.coerceAtLeast(1)
        val gridTop = tabY + tabH + 7 * u
        val margin = 6 * u
        val slot = (w - margin * 2) / cols
        c.grid.forEachIndexed { i, item ->
            val col = i % cols
            val row = i / cols
            val x = margin + col * slot
            val y = gridTop + row * (slot + 8 * u)
            val pad = 2 * u
            // slot: black outline + parchment face
            fillPaint.color = Color.BLACK
            canvas.drawRect(x, y, x + slot - pad, y + slot - pad, fillPaint)
            fillPaint.color = 0xFFB8A071.toInt()
            canvas.drawRect(
                x + u, y + u, x + slot - pad - u, y + slot - pad - u, fillPaint
            )
            // icon, nearest-neighbor
            icons[item.icon]?.let { bmp ->
                val box = slot - pad - 4 * u
                val s = minOf(box / bmp.width, box / bmp.height)
                val dw = bmp.width * s
                val dh = bmp.height * s
                val cx = x + (slot - pad) / 2
                val cy = y + (slot - pad) / 2 - u
                canvas.drawBitmap(
                    bmp, null,
                    RectF(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2),
                    crisp
                )
            }
            drawCenteredText(
                canvas, item.label, x + (slot - pad) / 2, y + slot + 0.5f * u,
                3.1f * u, tf, Color.BLACK
            )
        }

        // ---- THE progress bar (frozen graphics) ----
        val barW = 100 * u
        val barH = 11 * u
        drawPixelBar(
            canvas, (w - barW) / 2, h * 0.68f, barW, barH,
            c.barLabel, animProgress, c.accent, tf, u
        )

        // ---- hint ----
        if (c.hint.isNotEmpty()) {
            drawCenteredText(
                canvas, c.hint, w / 2, h - 7 * u,
                3.1f * u, tf, Color.argb(220, 60, 50, 35)
            )
        }
    }

    // ------------------------------------------------------------------
    // FROZEN: the progress bar. Do not restyle — only label, progress
    // and accent may vary per cartridge.
    // ------------------------------------------------------------------
    private fun drawPixelBar(
        canvas: Canvas, x: Float, top: Float, w: Float, h: Float,
        label: String, progress: Float, accent: Int, tf: Typeface, u: Float,
    ) {
        // label above
        drawCenteredText(canvas, label, x + w / 2, top - 6.5f * u, 6 * u, tf, Color.BLACK)

        // side nubs
        fillPaint.color = Color.BLACK
        canvas.drawRect(x - 3 * u, top + h * 0.25f, x, top + h * 0.75f, fillPaint)
        canvas.drawRect(x + w, top + h * 0.25f, x + w + 3 * u, top + h * 0.75f, fillPaint)

        // outer border
        canvas.drawRect(x, top, x + w, top + h, fillPaint)

        // track
        val ix = x + 1.5f * u
        val iy = top + 1.5f * u
        val iw = w - 3 * u
        val ih = h - 3 * u
        fillPaint.color = 0xFF2A2A2A.toInt()
        canvas.drawRect(ix, iy, ix + iw, iy + ih, fillPaint)

        // segments — frozen at 24
        val segs = 24
        val gap = 0.5f * u
        val segW = (iw - gap * (segs - 1)) / segs
        val filled = (progress * segs + 0.5f).toInt().coerceIn(0, segs)
        for (i in 0 until segs) {
            if (i >= filled) break
            val sx = ix + i * (segW + gap)
            fillPaint.color = accent
            canvas.drawRect(sx, iy, sx + segW, iy + ih, fillPaint)
            fillPaint.color = lighten(accent)
            canvas.drawRect(sx, iy, sx + segW, iy + ih * 0.35f, fillPaint)
        }

        // percent below
        val pct = "${(progress * 100 + 0.5f).toInt()}%"
        drawCenteredText(canvas, pct, x + w / 2, top + h + 6.5f * u, 6 * u, tf, Color.BLACK)
        // live progress label (e.g. "Screenshot 2 of 3") below the percent
        val c2 = cartridge
        if (c2 != null && c2.progressLabel.isNotEmpty()) {
            drawCenteredText(
                canvas, c2.progressLabel, x + w / 2, top + h + 12.5f * u,
                3.4f * u, tf, Color.argb(230, 40, 40, 40)
            )
        }
    }

    // ------------------------------------------------------------------

    private fun drawCenteredText(
        canvas: Canvas, text: String, cx: Float, cy: Float,
        size: Float, tf: Typeface, color: Int,
    ) {
        textPaint.typeface = tf
        textPaint.textSize = size
        textPaint.color = color
        textPaint.textAlign = Paint.Align.CENTER
        val fm = textPaint.fontMetrics
        canvas.drawText(text, cx, cy - (fm.ascent + fm.descent) / 2, textPaint)
    }

    private fun dim(color: Int): Int = Color.rgb(
        (Color.red(color) * 0.55f).toInt(),
        (Color.green(color) * 0.55f).toInt(),
        (Color.blue(color) * 0.55f).toInt()
    )

    private fun lighten(color: Int): Int = Color.rgb(
        minOf(255, (Color.red(color) * 1.35f).toInt()),
        minOf(255, (Color.green(color) * 1.35f).toInt()),
        minOf(255, (Color.blue(color) * 1.35f).toInt())
    )
}
