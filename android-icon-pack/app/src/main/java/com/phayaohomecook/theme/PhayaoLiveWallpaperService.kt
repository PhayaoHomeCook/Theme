package com.phayaohomecook.theme

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class PhayaoLiveWallpaperService : WallpaperService() {

    private data class Sparkle(
        var xNorm: Float,
        var yNorm: Float,
        var speedY: Float,
        var driftAmp: Float,
        var phase: Float,
        var radius: Float,
        var isBurst: Boolean = false,
        var life: Float = 1f
    )

    override fun onCreateEngine(): Engine = PhayaoLiveEngine()

    private inner class PhayaoLiveEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val frameDelayMs = 33L // ~30 fps silky-smooth, low battery

        private var visible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private var bgBitmap: Bitmap? = null
        private var mascotBitmap: Bitmap? = null
        private var startTimeMs = SystemClock.uptimeMillis()

        // Tap interaction state (happy hop + extra golden sparkle burst)
        private var tapBounceTimeMs = -10_000L

        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(85, 255, 232, 180)
            strokeCap = Paint.Cap.ROUND
        }
        private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val ambientSparkles = MutableList(22) {
            Sparkle(
                xNorm = Random.nextFloat(),
                yNorm = 0.40f + Random.nextFloat() * 0.56f,
                speedY = 0.012f + Random.nextFloat() * 0.024f,
                driftAmp = 0.008f + Random.nextFloat() * 0.018f,
                phase = Random.nextFloat() * (2f * PI.toFloat()),
                radius = 4f + Random.nextFloat() * 8f
            )
        }
        private val burstSparkles = mutableListOf<Sparkle>()

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    handler.postDelayed(this, frameDelayMs)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            bgBitmap = BitmapFactory.decodeResource(resources, R.drawable.live_bg_kwan_phayao)
            mascotBitmap = BitmapFactory.decodeResource(resources, R.drawable.live_mascot_wave)
            startTimeMs = SystemClock.uptimeMillis()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.post(drawRunnable)
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN && surfaceWidth > 0 && surfaceHeight > 0) {
                tapBounceTimeMs = SystemClock.uptimeMillis()
                val nx = event.x / surfaceWidth.toFloat()
                val ny = event.y / surfaceHeight.toFloat()
                for (i in 0 until 10) {
                    burstSparkles.add(
                        Sparkle(
                            xNorm = (nx + (Random.nextFloat() - 0.5f) * 0.14f).coerceIn(0.02f, 0.98f),
                            yNorm = (ny + (Random.nextFloat() - 0.5f) * 0.08f).coerceIn(0.05f, 0.95f),
                            speedY = 0.055f + Random.nextFloat() * 0.065f,
                            driftAmp = 0.02f + Random.nextFloat() * 0.025f,
                            phase = Random.nextFloat() * (2f * PI.toFloat()),
                            radius = 7f + Random.nextFloat() * 10f,
                            isBurst = true,
                            life = 1f
                        )
                    )
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onDestroy() {
            super.onDestroy()
            visible = false
            handler.removeCallbacks(drawRunnable)
            bgBitmap?.recycle()
            mascotBitmap?.recycle()
            bgBitmap = null
            mascotBitmap = null
        }

        private fun drawFrame() {
            val w = surfaceWidth
            val h = surfaceHeight
            if (w <= 0 || h <= 0) return

            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderScene(canvas, w, h)
                }
            } catch (_: Exception) {
                // Ignore transient surface states
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {}
                }
            }
        }

        private fun renderScene(canvas: Canvas, w: Int, h: Int) {
            val nowMs = SystemClock.uptimeMillis()
            val t = (nowMs - startTimeMs) / 1000f
            val dt = frameDelayMs / 1000f

            // 1. Draw Kwan Phayao deep burgundy background (center-crop)
            val bg = bgBitmap
            if (bg != null && !bg.isRecycled) {
                val scale = max(w.toFloat() / bg.width, h.toFloat() / bg.height)
                val drawW = bg.width * scale
                val drawH = bg.height * scale
                val left = (w - drawW) / 2f
                val top = (h - drawH) / 2f
                canvas.drawBitmap(bg, null, RectF(left, top, left + drawW, top + drawH), bitmapPaint)
            } else {
                canvas.drawColor(Color.rgb(78, 8, 34))
            }

            // 2. Soft shimmering golden water ripples on Kwan Phayao lake
            shimmerPaint.strokeWidth = max(2f, w * 0.0032f)
            for (i in 0 until 7) {
                val ly = h * (0.76f + i * 0.017f)
                val waveShift = sin(t * (1.2f + i * 0.18f) + i * 1.1f) * (w * 0.055f)
                val halfLen = w * (0.14f - i * 0.011f)
                val cx = w * 0.50f + waveShift
                val alpha = (48 + (sin(t * 2.1f + i) * 28f)).toInt().coerceIn(20, 95)
                shimmerPaint.color = Color.argb(alpha, 255, 228, 168)
                canvas.drawLine(cx - halfLen, ly, cx + halfLen, ly, shimmerPaint)
            }

            // 3. Soft pulsing golden sunrise aura behind the mascot
            val mascotSize = w * 0.84f
            val baseCenterX = w * 0.50f
            val baseBottomY = h * 0.885f
            val auraPulse = 1f + 0.05f * sin(t * 2.0f)
            val auraRadius = mascotSize * 0.52f * auraPulse
            val auraCenterY = baseBottomY - mascotSize * 0.44f
            auraPaint.shader = RadialGradient(
                baseCenterX,
                auraCenterY,
                auraRadius,
                intArrayOf(
                    Color.argb(82, 255, 218, 148),
                    Color.argb(32, 227, 173, 82),
                    Color.argb(0, 227, 173, 82)
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(baseCenterX, auraCenterY, auraRadius, auraPaint)

            // 4. Compute joyful idle bounce + sway + interactive tap hop
            val tapElapsed = (nowMs - tapBounceTimeMs) / 1000f
            val tapHop = if (tapElapsed in 0f..0.75f) {
                val p = tapElapsed / 0.75f
                sin(p * PI.toFloat()) * (h * 0.038f) * (1f - p * 0.25f)
            } else {
                0f
            }

            val idleBounce = sin(t * 2.5f) * (h * 0.0085f)
            val swayDeg = sin(t * 1.45f) * 2.6f + if (tapElapsed in 0f..0.75f) {
                sin(tapElapsed * 14f) * 3.5f * (1f - tapElapsed / 0.75f)
            } else {
                0f
            }
            val breatheX = 1f + sin(t * 2.5f) * 0.012f
            val breatheY = 1f - sin(t * 2.5f) * 0.014f

            // 5. Synced ground shadow beneath mascot shoes
            val liftFactor = ((idleBounce + tapHop) / (h * 0.045f)).coerceIn(-0.4f, 1.2f)
            val shadowW = mascotSize * (0.50f - liftFactor * 0.08f)
            val shadowH = mascotSize * (0.072f - liftFactor * 0.012f)
            val shadowY = baseBottomY - mascotSize * 0.055f
            shadowPaint.shader = RadialGradient(
                baseCenterX,
                shadowY,
                max(1f, shadowW * 0.5f),
                Color.argb((95 - liftFactor * 30).toInt().coerceIn(35, 105), 35, 3, 15),
                Color.argb(0, 35, 3, 15),
                Shader.TileMode.CLAMP
            )
            canvas.save()
            canvas.translate(baseCenterX, shadowY)
            canvas.scale(1f, shadowH / max(1f, shadowW))
            canvas.drawCircle(0f, 0f, shadowW * 0.5f, shadowPaint)
            canvas.restore()

            // 6. Draw Animated Waving Mascot (pivoted near feet so sway & bounce look natural!)
            val mascot = mascotBitmap
            if (mascot != null && !mascot.isRecycled) {
                canvas.save()
                val pivotX = baseCenterX
                val pivotY = baseBottomY - mascotSize * 0.08f - idleBounce - tapHop
                canvas.translate(pivotX, pivotY)
                canvas.rotate(swayDeg)
                canvas.scale(breatheX, breatheY)
                val halfW = mascotSize * 0.5f
                val dst = RectF(-halfW, -mascotSize * 0.92f, halfW, mascotSize * 0.08f)
                canvas.drawBitmap(mascot, null, dst, bitmapPaint)
                canvas.restore()
            }

            // 7. Ambient golden Lanna floating 4-point star sparkles
            for (s in ambientSparkles) {
                s.yNorm -= s.speedY * dt
                if (s.yNorm < 0.30f) {
                    s.yNorm = 0.94f + Random.nextFloat() * 0.04f
                    s.xNorm = Random.nextFloat()
                }
                val sx = (s.xNorm + sin(t * 1.4f + s.phase) * s.driftAmp) * w
                val sy = s.yNorm * h
                val twinkle = 0.35f + 0.65f * ((sin(t * 3.2f + s.phase) + 1f) * 0.5f)
                val fadeEdge = when {
                    s.yNorm < 0.42f -> ((s.yNorm - 0.30f) / 0.12f).coerceIn(0f, 1f)
                    s.yNorm > 0.88f -> ((0.96f - s.yNorm) / 0.08f).coerceIn(0f, 1f)
                    else -> 1f
                }
                val alpha = (210f * twinkle * fadeEdge).toInt().coerceIn(0, 230)
                drawFourPointStar(canvas, sx, sy, s.radius * (0.7f + 0.35f * twinkle), alpha)
            }

            // 8. Tap burst golden sparkles
            val it = burstSparkles.iterator()
            while (it.hasNext()) {
                val b = it.next()
                b.yNorm -= b.speedY * dt
                b.life -= dt * 1.35f
                if (b.life <= 0f) {
                    it.remove()
                    continue
                }
                val bx = (b.xNorm + cos(t * 5f + b.phase) * b.driftAmp) * w
                val by = b.yNorm * h
                val alpha = (245f * b.life).toInt().coerceIn(0, 255)
                drawFourPointStar(canvas, bx, by, b.radius * b.life, alpha)
            }
        }

        private fun drawFourPointStar(canvas: Canvas, cx: Float, cy: Float, r: Float, alpha: Int) {
            if (alpha <= 5 || r <= 0.5f) return
            starPaint.color = Color.argb(alpha, 250, 224, 148)
            val inner = r * 0.28f
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + inner, cy - inner)
                lineTo(cx + r, cy)
                lineTo(cx + inner, cy + inner)
                lineTo(cx, cy + r)
                lineTo(cx - inner, cy + inner)
                lineTo(cx - r, cy)
                lineTo(cx - inner, cy - inner)
                close()
            }
            canvas.drawPath(path, starPaint)
        }
    }
}
