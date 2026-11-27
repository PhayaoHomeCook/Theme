package com.phayaohomecook.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class PhayaoLiveWallpaperService : WallpaperService() {

    companion object {
        const val PREFS_NAME = "phayao_live_prefs"
        const val KEY_AUTO_CYCLE = "auto_cycle_poses"
        const val KEY_SPEECH_BUBBLE = "show_speech_bubble"
        const val KEY_LANTERNS = "show_lanna_lanterns"
        const val KEY_PARALLAX = "enable_tilt_parallax"
    }

    private data class Sparkle(
        var xNorm: Float,
        var yNorm: Float,
        var speedY: Float,
        var driftAmp: Float,
        var phase: Float,
        var radius: Float,
        var isBurst: Boolean = false,
        var isHeart: Boolean = false,
        var life: Float = 1f
    )

    private data class Lantern(
        var xNorm: Float,
        var yNorm: Float,
        var speedY: Float,
        var swayAmp: Float,
        var phase: Float,
        var scale: Float
    )

    private data class SteamPuff(
        var offsetX: Float,
        var offsetY: Float,
        var speedY: Float,
        var phase: Float,
        var radius: Float,
        var life: Float
    )

    override fun onCreateEngine(): Engine = PhayaoLiveEngine()

    private inner class PhayaoLiveEngine : Engine(),
        SensorEventListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

        private val handler = Handler(Looper.getMainLooper())
        private val frameDelayMs = 33L // ~30 fps smooth & battery-friendly

        private var visible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private var bgBitmap: Bitmap? = null
        private var poseBitmaps: Array<Bitmap?> = arrayOfNulls(4)
        private var currentPoseIndex = 0
        private var lastPoseSwitchMs = SystemClock.uptimeMillis()
        private var startTimeMs = SystemClock.uptimeMillis()

        // Interactive tap jump & pose transition
        private var tapBounceTimeMs = -10_000L

        // Thai speech bubble state
        private var speechText = ""
        private var speechShowMs = -10_000L
        private val speechDurationMs = 3600L

        // Accelerometer 3D tilt parallax state
        private var sensorManager: SensorManager? = null
        private var accelerometer: Sensor? = null
        private var tiltX = 0f
        private var tiltY = 0f
        private var smoothTiltX = 0f
        private var smoothTiltY = 0f

        // Live customizable preferences
        private lateinit var prefs: SharedPreferences
        private var autoCycleEnabled = true
        private var speechBubbleEnabled = true
        private var lanternsEnabled = true
        private var parallaxEnabled = true

        // Paints
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
        private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val heartStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.rgb(247, 218, 148)
        }
        private val lanternGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val lanternBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val steamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val bubbleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        private val ambientSparkles = MutableList(20) {
            Sparkle(
                xNorm = Random.nextFloat(),
                yNorm = 0.36f + Random.nextFloat() * 0.58f,
                speedY = 0.012f + Random.nextFloat() * 0.022f,
                driftAmp = 0.008f + Random.nextFloat() * 0.016f,
                phase = Random.nextFloat() * (2f * PI.toFloat()),
                radius = 4f + Random.nextFloat() * 7.5f
            )
        }

        private val skyLanterns = MutableList(6) { idx ->
            Lantern(
                xNorm = 0.08f + (idx / 6f) * 0.84f + (Random.nextFloat() - 0.5f) * 0.06f,
                yNorm = 0.14f + Random.nextFloat() * 0.44f,
                speedY = 0.006f + Random.nextFloat() * 0.009f,
                swayAmp = 0.012f + Random.nextFloat() * 0.016f,
                phase = Random.nextFloat() * (2f * PI.toFloat()),
                scale = 0.65f + Random.nextFloat() * 0.55f
            )
        }

        private val burstParticles = mutableListOf<Sparkle>()
        private val steamPuffs = mutableListOf<SteamPuff>()
        private var lastSteamSpawnMs = 0L

        private val poseQuotes = arrayOf(
            "พะเยาโฮมคุก ยินดีต้อนรับค่ะ ✨",
            "อร่อยแน่นอนค่ะ ยกนิ้วให้เลย! 👍",
            "ของอร่อยร้อนๆ พร้อมเสิร์ฟค่ะ 🍜",
            "ส่งรักจากครัวบ้านเรานะคะ ♡"
        )

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

            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPreferences()
            prefs.registerOnSharedPreferenceChangeListener(this)

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            bgBitmap = BitmapFactory.decodeResource(resources, R.drawable.live_bg_kwan_phayao)
            poseBitmaps = arrayOf(
                BitmapFactory.decodeResource(resources, R.drawable.live_mascot_wave),
                BitmapFactory.decodeResource(resources, R.drawable.live_mascot_thumbsup),
                BitmapFactory.decodeResource(resources, R.drawable.live_mascot_serving),
                BitmapFactory.decodeResource(resources, R.drawable.live_mascot_hearts)
            )
            startTimeMs = SystemClock.uptimeMillis()
            lastPoseSwitchMs = startTimeMs
        }

        private fun loadPreferences() {
            autoCycleEnabled = prefs.getBoolean(KEY_AUTO_CYCLE, true)
            speechBubbleEnabled = prefs.getBoolean(KEY_SPEECH_BUBBLE, true)
            lanternsEnabled = prefs.getBoolean(KEY_LANTERNS, true)
            parallaxEnabled = prefs.getBoolean(KEY_PARALLAX, true)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            loadPreferences()
            if (visible) {
                updateSensorRegistration(true)
                drawFrame()
            }
        }

        private fun updateSensorRegistration(enable: Boolean) {
            val sm = sensorManager ?: return
            sm.unregisterListener(this)
            if (enable && parallaxEnabled && accelerometer != null) {
                sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            } else {
                tiltX = 0f
                tiltY = 0f
            }
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Clamp gentle tilt range (-4..4 m/s^2 -> -1..1)
                tiltX = (-event.values[0] / 4.2f).coerceIn(-1f, 1f)
                tiltY = ((event.values[1] - 5.5f) / 4.5f).coerceIn(-1f, 1f)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(drawRunnable)
            updateSensorRegistration(visible)
            if (visible) {
                // Show friendly time-of-day greeting briefly on wake!
                if (speechBubbleEnabled) {
                    showGreeting(getTimeOfDayGreeting())
                }
                handler.post(drawRunnable)
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN && surfaceWidth > 0 && surfaceHeight > 0) {
                val now = SystemClock.uptimeMillis()
                tapBounceTimeMs = now
                // Switch to next unique mascot pose on every tap!
                currentPoseIndex = (currentPoseIndex + 1) % poseBitmaps.size
                lastPoseSwitchMs = now

                if (speechBubbleEnabled) {
                    showGreeting(poseQuotes[currentPoseIndex])
                }

                val nx = event.x / surfaceWidth.toFloat()
                val ny = event.y / surfaceHeight.toFloat()
                val spawnHearts = (currentPoseIndex == 3)
                for (i in 0 until 12) {
                    burstParticles.add(
                        Sparkle(
                            xNorm = (nx + (Random.nextFloat() - 0.5f) * 0.16f).coerceIn(0.03f, 0.97f),
                            yNorm = (ny + (Random.nextFloat() - 0.5f) * 0.10f).coerceIn(0.06f, 0.94f),
                            speedY = 0.055f + Random.nextFloat() * 0.065f,
                            driftAmp = 0.020f + Random.nextFloat() * 0.025f,
                            phase = Random.nextFloat() * (2f * PI.toFloat()),
                            radius = if (spawnHearts) 10f + Random.nextFloat() * 9f else 7f + Random.nextFloat() * 10f,
                            isBurst = true,
                            isHeart = spawnHearts || (i % 4 == 0),
                            life = 1f
                        )
                    )
                }
            }
        }

        private fun showGreeting(text: String) {
            speechText = text
            speechShowMs = SystemClock.uptimeMillis()
        }

        private fun getTimeOfDayGreeting(): String {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..10 -> "อรุณสวัสดิ์ค่ะ! ทานมื้อเช้ากันนะคะ ☀️"
                in 11..13 -> "มื้อเที่ยงแล้ว ทานให้อร่อยนะคะ 🍜"
                in 14..16 -> "บ่ายนี้เติมความหวานกันไหมคะ 💛"
                in 17..20 -> "เย็นนี้ทานอะไรดีคะ? พะเยาโฮมคุก ✨"
                else -> "ฝันดีนะคะ พรุ่งนี้เจอกันใหม่ค่ะ 🌙"
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
            updateSensorRegistration(false)
        }

        override fun onDestroy() {
            super.onDestroy()
            visible = false
            handler.removeCallbacks(drawRunnable)
            updateSensorRegistration(false)
            try {
                prefs.unregisterOnSharedPreferenceChangeListener(this)
            } catch (_: Exception) {}
            bgBitmap?.recycle()
            bgBitmap = null
            for (i in poseBitmaps.indices) {
                poseBitmaps[i]?.recycle()
                poseBitmaps[i] = null
            }
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

            // Auto-cycle mascot pose softly every 12 seconds if enabled
            if (autoCycleEnabled && (nowMs - lastPoseSwitchMs) >= 12_000L) {
                currentPoseIndex = (currentPoseIndex + 1) % poseBitmaps.size
                lastPoseSwitchMs = nowMs
                tapBounceTimeMs = nowMs
            }

            // Smooth low-pass filter for silky 3D tilt parallax
            val targetTx = if (parallaxEnabled) tiltX else 0f
            val targetTy = if (parallaxEnabled) tiltY else 0f
            smoothTiltX += (targetTx - smoothTiltX) * 0.12f
            smoothTiltY += (targetTy - smoothTiltY) * 0.12f
            val bgParallaxX = smoothTiltX * (w * 0.022f)
            val bgParallaxY = smoothTiltY * (h * 0.012f)
            val mascotParallaxX = -smoothTiltX * (w * 0.028f)
            val mascotParallaxY = -smoothTiltY * (h * 0.014f)

            // 1. Draw Kwan Phayao background slightly overscaled for seamless 3D parallax
            val bg = bgBitmap
            if (bg != null && !bg.isRecycled) {
                val scale = max(w.toFloat() / bg.width, h.toFloat() / bg.height) * 1.06f
                val drawW = bg.width * scale
                val drawH = bg.height * scale
                val left = (w - drawW) / 2f + bgParallaxX
                val top = (h - drawH) / 2f + bgParallaxY
                canvas.drawBitmap(bg, null, RectF(left, top, left + drawW, top + drawH), bitmapPaint)
            } else {
                canvas.drawColor(Color.rgb(78, 8, 34))
            }

            // 2. Floating Lanna Sky Lanterns (โคมลอยล้านนา) in background sky
            if (lanternsEnabled) {
                for (l in skyLanterns) {
                    l.yNorm -= l.speedY * dt
                    if (l.yNorm < 0.06f) {
                        l.yNorm = 0.62f + Random.nextFloat() * 0.08f
                        l.xNorm = 0.08f + Random.nextFloat() * 0.84f
                    }
                    val lx = (l.xNorm + sin(t * 0.8f + l.phase) * l.swayAmp) * w + bgParallaxX * 0.6f
                    val ly = l.yNorm * h + bgParallaxY * 0.6f
                    val edgeFade = when {
                        l.yNorm < 0.16f -> ((l.yNorm - 0.06f) / 0.10f).coerceIn(0f, 1f)
                        l.yNorm > 0.56f -> ((0.66f - l.yNorm) / 0.10f).coerceIn(0f, 1f)
                        else -> 1f
                    }
                    val flicker = 0.82f + 0.18f * sin(t * 4.5f + l.phase)
                    drawLannaLantern(canvas, lx, ly, w * 0.022f * l.scale, edgeFade * flicker)
                }
            }

            // 3. Shimmering golden water ripples on Kwan Phayao lake
            shimmerPaint.strokeWidth = max(2f, w * 0.0032f)
            for (i in 0 until 7) {
                val ly = h * (0.76f + i * 0.017f) + bgParallaxY * 0.4f
                val waveShift = sin(t * (1.2f + i * 0.18f) + i * 1.1f) * (w * 0.055f)
                val halfLen = w * (0.14f - i * 0.011f)
                val cx = w * 0.50f + waveShift + bgParallaxX * 0.4f
                val alpha = (48 + (sin(t * 2.1f + i) * 28f)).toInt().coerceIn(20, 95)
                shimmerPaint.color = Color.argb(alpha, 255, 228, 168)
                canvas.drawLine(cx - halfLen, ly, cx + halfLen, ly, shimmerPaint)
            }

            // 4. Pulsing warm golden aura behind mascot
            val mascotSize = w * 0.84f
            val baseCenterX = w * 0.50f + mascotParallaxX
            val baseBottomY = h * 0.885f + mascotParallaxY
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

            // 5. Joyful idle bounce + sway + interactive tap hop
            val tapElapsed = (nowMs - tapBounceTimeMs) / 1000f
            val tapHop = if (tapElapsed in 0f..0.72f) {
                val p = tapElapsed / 0.72f
                sin(p * PI.toFloat()) * (h * 0.040f) * (1f - p * 0.22f)
            } else {
                0f
            }
            val popScale = if (tapElapsed in 0f..0.32f) {
                val p = tapElapsed / 0.32f
                1f + sin(p * PI.toFloat()) * 0.065f
            } else {
                1f
            }

            val idleBounce = sin(t * 2.5f) * (h * 0.0085f)
            val swayDeg = sin(t * 1.45f) * 2.6f + if (tapElapsed in 0f..0.72f) {
                sin(tapElapsed * 14f) * 3.8f * (1f - tapElapsed / 0.72f)
            } else {
                0f
            }
            val breatheX = (1f + sin(t * 2.5f) * 0.012f) * popScale
            val breatheY = (1f - sin(t * 2.5f) * 0.014f) * popScale

            // 6. Synced ground shadow beneath mascot shoes
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

            // 7. Draw Active Animated Mascot Pose
            val mascot = poseBitmaps[currentPoseIndex] ?: poseBitmaps[0]
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

            // 8. Live rising aromatic steam puffs when in Serving Dish pose (index 2)
            if (currentPoseIndex == 2 && (nowMs - lastSteamSpawnMs) > 420L) {
                lastSteamSpawnMs = nowMs
                steamPuffs.add(
                    SteamPuff(
                        offsetX = -mascotSize * 0.34f + (Random.nextFloat() - 0.5f) * mascotSize * 0.07f,
                        offsetY = -mascotSize * 0.48f,
                        speedY = mascotSize * (0.09f + Random.nextFloat() * 0.05f),
                        phase = Random.nextFloat() * (2f * PI.toFloat()),
                        radius = mascotSize * (0.022f + Random.nextFloat() * 0.016f),
                        life = 1f
                    )
                )
            }
            val steamIt = steamPuffs.iterator()
            while (steamIt.hasNext()) {
                val sp = steamIt.next()
                sp.offsetY -= sp.speedY * dt
                sp.life -= dt * 0.72f
                if (sp.life <= 0f) {
                    steamIt.remove()
                    continue
                }
                val px = baseCenterX + sp.offsetX + sin(t * 3.0f + sp.phase) * (mascotSize * 0.022f)
                val py = baseBottomY + sp.offsetY - idleBounce
                val alpha = (95f * sin(sp.life * PI.toFloat())).toInt().coerceIn(0, 110)
                steamPaint.color = Color.argb(alpha, 255, 245, 228)
                canvas.drawCircle(px, py, sp.radius * (1.35f - sp.life * 0.45f), steamPaint)
            }

            // 9. Cute Thai Speech Bubble above mascot (fades smoothly in & out)
            if (speechBubbleEnabled && speechText.isNotEmpty()) {
                val elapsedBubble = nowMs - speechShowMs
                if (elapsedBubble in 0L..speechDurationMs) {
                    val alphaNorm = when {
                        elapsedBubble < 240L -> elapsedBubble / 240f
                        elapsedBubble > speechDurationMs - 520L -> (speechDurationMs - elapsedBubble) / 520f
                        else -> 1f
                    }.coerceIn(0f, 1f)
                    val floatUp = (1f - alphaNorm) * 14f
                    drawSpeechBubble(
                        canvas,
                        w,
                        baseCenterX,
                        baseBottomY - mascotSize * 0.90f - idleBounce - tapHop - floatUp,
                        speechText,
                        alphaNorm
                    )
                }
            }

            // 10. Foreground ambient golden sparkles
            if (lanternsEnabled) {
                for (s in ambientSparkles) {
                    s.yNorm -= s.speedY * dt
                    if (s.yNorm < 0.28f) {
                        s.yNorm = 0.94f + Random.nextFloat() * 0.04f
                        s.xNorm = Random.nextFloat()
                    }
                    val sx = (s.xNorm + sin(t * 1.4f + s.phase) * s.driftAmp) * w - bgParallaxX * 0.5f
                    val sy = s.yNorm * h
                    val twinkle = 0.35f + 0.65f * ((sin(t * 3.2f + s.phase) + 1f) * 0.5f)
                    val fadeEdge = when {
                        s.yNorm < 0.40f -> ((s.yNorm - 0.28f) / 0.12f).coerceIn(0f, 1f)
                        s.yNorm > 0.88f -> ((0.96f - s.yNorm) / 0.08f).coerceIn(0f, 1f)
                        else -> 1f
                    }
                    val alpha = (210f * twinkle * fadeEdge).toInt().coerceIn(0, 230)
                    drawFourPointStar(canvas, sx, sy, s.radius * (0.7f + 0.35f * twinkle), alpha)
                }
            }

            // 11. Tap burst particles (Stars + Floating Pink-Gold Hearts)
            val it = burstParticles.iterator()
            while (it.hasNext()) {
                val b = it.next()
                b.yNorm -= b.speedY * dt
                b.life -= dt * 1.25f
                if (b.life <= 0f) {
                    it.remove()
                    continue
                }
                val bx = (b.xNorm + cos(t * 4.5f + b.phase) * b.driftAmp) * w
                val by = b.yNorm * h
                val alpha = (245f * b.life).toInt().coerceIn(0, 255)
                if (b.isHeart) {
                    drawHeart(canvas, bx, by, b.radius * (0.65f + 0.45f * b.life), alpha)
                } else {
                    drawFourPointStar(canvas, bx, by, b.radius * b.life, alpha)
                }
            }
        }

        private fun drawLannaLantern(canvas: Canvas, cx: Float, cy: Float, r: Float, intensity: Float) {
            if (intensity <= 0.03f) return
            val glowR = r * 2.8f
            lanternGlowPaint.shader = RadialGradient(
                cx, cy, glowR,
                intArrayOf(
                    Color.argb((125 * intensity).toInt().coerceIn(0, 150), 255, 206, 112),
                    Color.argb((45 * intensity).toInt().coerceIn(0, 65), 255, 150, 62),
                    Color.argb(0, 255, 150, 62)
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, glowR, lanternGlowPaint)

            // Lantern body (warm amber oval-trapezoid)
            val bodyRect = RectF(cx - r * 0.65f, cy - r * 0.95f, cx + r * 0.65f, cy + r * 0.85f)
            lanternBodyPaint.color = Color.argb((215 * intensity).toInt().coerceIn(0, 235), 255, 212, 128)
            canvas.drawRoundRect(bodyRect, r * 0.45f, r * 0.45f, lanternBodyPaint)

            // Warm bright inner flame core
            lanternBodyPaint.color = Color.argb((240 * intensity).toInt().coerceIn(0, 255), 255, 248, 212)
            canvas.drawCircle(cx, cy + r * 0.25f, r * 0.30f, lanternBodyPaint)
        }

        private fun drawSpeechBubble(canvas: Canvas, screenW: Int, cx: Float, bottomY: Float, text: String, alphaNorm: Float) {
            val a = (255 * alphaNorm).toInt().coerceIn(0, 255)
            if (a <= 8) return
            bubbleTextPaint.textSize = max(24f, screenW * 0.034f)
            val textW = bubbleTextPaint.measureText(text)
            val padH = screenW * 0.048f
            val padV = screenW * 0.028f
            val boxW = (textW + padH * 2f).coerceAtMost(screenW * 0.86f)
            val boxH = bubbleTextPaint.textSize + padV * 2f
            val clampedCx = cx.coerceIn(boxW * 0.5f + 20f, screenW - boxW * 0.5f - 20f)
            val rect = RectF(clampedCx - boxW * 0.5f, bottomY - boxH, clampedCx + boxW * 0.5f, bottomY)

            bubbleFillPaint.color = Color.argb((238 * alphaNorm).toInt(), 252, 245, 229)
            bubbleStrokePaint.color = Color.argb(a, 227, 173, 82)
            bubbleTextPaint.color = Color.argb(a, 78, 8, 34)

            canvas.drawRoundRect(rect, boxH * 0.48f, boxH * 0.48f, bubbleFillPaint)
            canvas.drawRoundRect(rect, boxH * 0.48f, boxH * 0.48f, bubbleStrokePaint)

            // Small speech pointer triangle
            val tail = Path().apply {
                moveTo(clampedCx - 14f, bottomY - 2f)
                lineTo(clampedCx + 14f, bottomY - 2f)
                lineTo(clampedCx, bottomY + 14f)
                close()
            }
            canvas.drawPath(tail, bubbleFillPaint)

            val fm = bubbleTextPaint.fontMetrics
            val textY = rect.centerY() - (fm.ascent + fm.descent) * 0.5f
            canvas.drawText(text, clampedCx, textY, bubbleTextPaint)
        }

        private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, r: Float, alpha: Int) {
            if (alpha <= 5 || r <= 1f) return
            heartPaint.color = Color.argb(alpha, 232, 86, 134)
            heartStrokePaint.color = Color.argb(alpha, 247, 218, 148)
            heartStrokePaint.strokeWidth = max(1.5f, r * 0.14f)
            val path = Path().apply {
                moveTo(cx, cy + r * 0.75f)
                cubicTo(cx - r * 1.15f, cy + r * 0.05f, cx - r * 0.95f, cy - r * 0.90f, cx, cy - r * 0.25f)
                cubicTo(cx + r * 0.95f, cy - r * 0.90f, cx + r * 1.15f, cy + r * 0.05f, cx, cy + r * 0.75f)
                close()
            }
            canvas.drawPath(path, heartPaint)
            canvas.drawPath(path, heartStrokePaint)
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
