package com.mosque.prayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mosque.prayer.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnimatedTicksView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    var onBottomCoveredChanged: ((Boolean) -> Unit)? = null
    private var lastBottomCovered: Boolean = false

    // Optional provider for "now" to drive the animation. When set, the
    // luminous segment will align with the caller's notion of time (e.g. the
    // simulated clock that starts with seconds=0 when the user changes time),
    // ensuring the bright segment begins at the top.
    private var nowProvider: (() -> Long)? = null

    fun setNowProvider(provider: (() -> Long)?) {
        nowProvider = provider
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Align the tick frame with the black panel (imgBackground), which
        // is inset 24dp from each edge in the root layout. We use the same
        // inset so the ticks sit in the white border area around the panel.
        val density = resources.displayMetrics.density
        val panelMargin = 24f * density
        val left = panelMargin
        val top = panelMargin
        val right = w - panelMargin
        val bottom = h - panelMargin

        val innerW = (right - left)
        val innerH = (bottom - top)
        val minDim = min(innerW, innerH)
        val panelCorner = resources.getDimension(R.dimen.card_radius)
        val cornerR = min(panelCorner, minDim / 2f)
        val straightW = (innerW - 2f * cornerR).coerceAtLeast(1f)
        val straightH = (innerH - 2f * cornerR).coerceAtLeast(1f)
        val arcLen = (PI.toFloat() / 2f) * cornerR
        val perim = 2f * (straightW + straightH) + 4f * arcLen

        // Draw ticks in black so they are visible on the white border
        val color = ContextCompat.getColor(context, R.color.black)
        paint.color = color
        paint.strokeWidth = minDim * 0.0055f

        // Many ticks so they are visually very close together,
        // and drawn fully *inside* the black panel so they do not
        // spill over the outer white background.
        val ticks = 180
        val tickLen = minDim * 0.022f
        // Taille de la portion lumineuse ("fil" qui tourne) : plus la valeur est grande,
        // plus la zone brillante est longue. On la réduit pour avoir environ la moitié
        // de la longueur initiale.
        val activeWindow = 15
        val skipModulo = 5
        val brightSkipModulo = 3

        var bottomCovered = false
        val centerX = (left + right) / 2f
        val bottomBandHalfWidth = (right - left) * 0.14f
        val bottomY = bottom

        // One full revolution per minute, but advance in discrete seconds so
        // movement steps match seconds and the bright segment reaches TOP at each new minute.
        val tNow = nowProvider?.invoke() ?: System.currentTimeMillis()
        val secs = ((tNow / 1000L) % 60L).toInt()

        // Tick index corresponding to the TOP-center of the frame.
        val topCenterTick = (((straightW / 2f) / perim) * ticks.toFloat()).toInt()
        // Center of the bright window advances 3 ticks per second (ticks/60),
        // so at secs==0 it sits exactly at TOP, and completes a revolution in 60s.
        val centerTick = (topCenterTick + ((secs * ticks) / 60)) % ticks
        val start = (centerTick - activeWindow / 2 + ticks) % ticks
        val oppositeCenter = (centerTick + ticks / 2) % ticks
        val oppositeWindow = activeWindow
        // Size of the grey-free gap immediately *after* the bright window
        // in the drawing/rotation direction. This ensures no dim ticks are
        // seen in front of the luminous segment.
        val leadGap = activeWindow * 5

        for (i in 0 until ticks) {
            val relActive = (i - start + ticks) % ticks
            val relOpp = (i - oppositeCenter + ticks) % ticks
            // Distance from the end of the bright window in tick indices;
            // ticks immediately after the bright window have small relAhead.
            val relAhead = (i - (start + activeWindow) + ticks) % ticks
            val baseAlpha = 55

            // Tick currently in the bright rotating window?
            val isActive = relActive in 0 until activeWindow

            if (!isActive && (i % skipModulo != 0)) {
                continue
            }

            val alpha = when {
                isActive && (relActive % brightSkipModulo != 0) -> 0
                isActive -> {
                    (255f * (relActive / activeWindow.toFloat())).toInt()
                        .coerceIn(80, 255)
                }
                // Completely hide ticks only in the grey-free gap
                // immediately *ahead* of the bright window.
                relAhead in 0 until leadGap -> 0
                else -> baseAlpha
            }
            paint.alpha = alpha

            val t = (i.toFloat() / ticks.toFloat()) * perim
            val (px, py, nx, ny) = pointAndNormalOnRoundedRect(
                t = t,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                r = cornerR,
                straightW = straightW,
                straightH = straightH,
                arcLen = arcLen
            )

            // Normals from pointAndNormalOnRoundedRect point *inside* the panel.
            // To draw ticks in the white border (outside the black panel), we
            // step in the opposite direction of the normal.
            val x1 = px
            val y1 = py
            val x2 = px - nx * tickLen
            val y2 = py - ny * tickLen
            canvas.drawLine(x1, y1, x2, y2, paint)

            if (!bottomCovered && relActive in 0 until activeWindow) {
                if (py >= bottomY - 1f && abs(px - centerX) <= bottomBandHalfWidth) {
                    bottomCovered = true
                }
            }
        }
        if (bottomCovered != lastBottomCovered) {
            lastBottomCovered = bottomCovered
            onBottomCoveredChanged?.invoke(bottomCovered)
        }
        postInvalidateOnAnimation()
    }

    private fun pointAndNormalOnRoundedRect(
        t: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        r: Float,
        straightW: Float,
        straightH: Float,
        arcLen: Float
    ): FloatArray {
        val tlCx = left + r
        val tlCy = top + r
        val trCx = right - r
        val trCy = top + r
        val brCx = right - r
        val brCy = bottom - r
        val blCx = left + r
        val blCy = bottom - r

        var d = t

        // Top edge (left->right)
        if (d < straightW) {
            val x = (left + r) + d
            val y = top
            return floatArrayOf(x, y, 0f, 1f)
        }
        d -= straightW

        // Top-right arc (270deg -> 360deg)
        if (d < arcLen) {
            val ang = (3f * PI.toFloat() / 2f) + (d / arcLen) * (PI.toFloat() / 2f)
            val x = trCx + r * cos(ang)
            val y = trCy + r * sin(ang)
            val nx = -cos(ang)
            val ny = -sin(ang)
            return floatArrayOf(x, y, nx, ny)
        }
        d -= arcLen

        // Right edge (top->bottom)
        if (d < straightH) {
            val x = right
            val y = (top + r) + d
            return floatArrayOf(x, y, -1f, 0f)
        }
        d -= straightH

        // Bottom-right arc (0deg -> 90deg)
        if (d < arcLen) {
            val ang = 0f + (d / arcLen) * (PI.toFloat() / 2f)
            val x = brCx + r * cos(ang)
            val y = brCy + r * sin(ang)
            val nx = -cos(ang)
            val ny = -sin(ang)
            return floatArrayOf(x, y, nx, ny)
        }
        d -= arcLen

        // Bottom edge (right->left)
        if (d < straightW) {
            val x = (right - r) - d
            val y = bottom
            return floatArrayOf(x, y, 0f, -1f)
        }
        d -= straightW

        // Bottom-left arc (90deg -> 180deg)
        if (d < arcLen) {
            val ang = (PI.toFloat() / 2f) + (d / arcLen) * (PI.toFloat() / 2f)
            val x = blCx + r * cos(ang)
            val y = blCy + r * sin(ang)
            val nx = -cos(ang)
            val ny = -sin(ang)
            return floatArrayOf(x, y, nx, ny)
        }
        d -= arcLen

        // Left edge (bottom->top)
        if (d < straightH) {
            val x = left
            val y = (bottom - r) - d
            return floatArrayOf(x, y, 1f, 0f)
        }
        d -= straightH

        // Top-left arc (180deg -> 270deg)
        val ang = PI.toFloat() + (d / arcLen) * (PI.toFloat() / 2f)
        val x = tlCx + r * cos(ang)
        val y = tlCy + r * sin(ang)
        val nx = -cos(ang)
        val ny = -sin(ang)
        return floatArrayOf(x, y, nx, ny)
    }
}
