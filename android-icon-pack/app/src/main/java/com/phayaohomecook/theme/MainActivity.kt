package com.phayaohomecook.theme

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bgCream = ContextCompat.getColor(this, R.color.phayao_cream)
        val bgSand = ContextCompat.getColor(this, R.color.phayao_sand)
        val burgundy = ContextCompat.getColor(this, R.color.phayao_burgundy)
        val burgundyDark = ContextCompat.getColor(this, R.color.phayao_burgundy_dark)
        val roseAccent = ContextCompat.getColor(this, R.color.phayao_rose_accent)
        val gold = ContextCompat.getColor(this, R.color.phayao_gold)
        val goldLight = ContextCompat.getColor(this, R.color.phayao_gold_light)
        val textDark = ContextCompat.getColor(this, R.color.phayao_text_dark)
        val textMuted = ContextCompat.getColor(this, R.color.phayao_text_muted)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bgCream)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 56, 44, 72)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Top Burgundy Banner Card with Mascot
        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 40, 36, 36)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(burgundy, burgundyDark)
            ).apply {
                cornerRadius = 42f
                setStroke(4, gold)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 32 }
        }

        val mascotView = ImageView(this).apply {
            setImageResource(R.drawable.ic_phayao_mascot)
            layoutParams = LinearLayout.LayoutParams(220, 220).apply { bottomMargin = 18 }
        }
        headerCard.addView(mascotView)

        headerCard.addView(TextView(this).apply {
            text = getString(R.string.theme_title)
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(goldLight)
            gravity = Gravity.CENTER
        })

        headerCard.addView(TextView(this).apply {
            text = getString(R.string.theme_subtitle)
            textSize = 13f
            setTextColor(bgCream)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 20)
        })

        // Mini preview strip of 5 custom icons inside header
        val iconStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val sampleIcons = intArrayOf(
            R.drawable.ic_line,
            R.drawable.ic_phone,
            R.drawable.ic_camera,
            R.drawable.ic_food,
            R.drawable.ic_lanna_flower
        )
        for (resId in sampleIcons) {
            iconStrip.addView(ImageView(this).apply {
                setImageResource(resId)
                layoutParams = LinearLayout.LayoutParams(96, 96).apply {
                    marginStart = 8
                    marginEnd = 8
                }
            })
        }
        headerCard.addView(iconStrip)
        root.addView(headerCard)

        fun createButton(label: String, bgColor: Int, textColor: Int = bgCream, onClick: () -> Unit): Button {
            return Button(this).apply {
                text = label
                setTextColor(textColor)
                textSize = 14.5f
                setTypeface(null, Typeface.BOLD)
                isAllCaps = false
                background = GradientDrawable().apply {
                    cornerRadius = 32f
                    setColor(bgColor)
                    setStroke(3, gold)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 20 }
                setPadding(28, 30, 28, 30)
                setOnClickListener { onClick() }
            }
        }

        // 1-Tap Full Wallpaper Bundle (Lock + Home Light)
        root.addView(createButton(getString(R.string.btn_apply_pack_light), burgundy) {
            applyBothWallpapers(R.drawable.wallpaper_lockscreen, R.drawable.wallpaper_homescreen_light, "โทนครีมอบอุ่น")
        })

        // 1-Tap Full Wallpaper Bundle (Lock + Home Dark)
        root.addView(createButton(getString(R.string.btn_apply_pack_dark), burgundyDark) {
            applyBothWallpapers(R.drawable.wallpaper_lockscreen, R.drawable.wallpaper_homescreen_dark, "โทนเลือดหมูเข้ม")
        })

        // 1-Tap Apply Icon Pack to installed Launcher
        root.addView(createButton(getString(R.string.btn_apply_icons), roseAccent) {
            applyIconPackToLauncher()
        })

        // 1-Tap Save Keyboard & Chat Wallpapers to vivo Gallery
        root.addView(createButton(getString(R.string.btn_save_kb_chat), bgSand, burgundyDark) {
            saveKeyboardAndChatToGallery()
        })

        // Quick shortcut to Gboard settings
        root.addView(createButton(getString(R.string.btn_open_gboard), bgSand, textDark) {
            openGboardOrInputSettings()
        })

        // Quick shortcut to Display / Wallpaper settings (for vivo UI Color)
        root.addView(createButton(getString(R.string.btn_open_ui_color), bgSand, textDark) {
            try {
                startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })

        // Helpful Vivo Tip Card
        root.addView(TextView(this).apply {
            text = getString(R.string.icon_pack_tip)
            textSize = 12.5f
            setLineSpacing(6f, 1.0f)
            setTextColor(textMuted)
            background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(bgSand)
                setStroke(2, gold)
            }
            setPadding(34, 28, 34, 28)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
        })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun applyBothWallpapers(lockRes: Int, homeRes: Int, label: String) {
        Toast.makeText(this, "กำลังตั้งค่าวอลเปเปอร์$label...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val wm = WallpaperManager.getInstance(this)
                val lockBmp = BitmapFactory.decodeResource(resources, lockRes)
                val homeBmp = BitmapFactory.decodeResource(resources, homeRes)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setBitmap(lockBmp, null, true, WallpaperManager.FLAG_LOCK)
                    wm.setBitmap(homeBmp, null, true, WallpaperManager.FLAG_SYSTEM)
                } else {
                    wm.setBitmap(homeBmp)
                }
                lockBmp.recycle()
                homeBmp.recycle()
                runOnUiThread {
                    Toast.makeText(this, "ตั้งค่าหน้าจอล็อก + หน้าหลัก ($label) เรียบร้อยแล้ว!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "ไม่สามารถตั้งวอลเปเปอร์ได้: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun applyIconPackToLauncher() {
        val pkg = packageName
        // Try Nova Launcher
        if (isInstalled("com.teslacoilsw.launcher")) {
            val intent = Intent("com.teslacoilsw.launcher.APPLY_ICON_THEME").apply {
                setPackage("com.teslacoilsw.launcher")
                putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_TYPE", "GO")
                putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE", pkg)
            }
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        // Try Smart Launcher
        if (isInstalled("ginlemon.flowerfree") || isInstalled("ginlemon.flowerpro")) {
            val intent = Intent("ginlemon.smartlauncher.setGSLTHEME").apply {
                putExtra("package", pkg)
            }
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        // Try Lawnchair
        for (lcPkg in listOf("app.lawnchair", "ch.deletescape.lawnchair.plah")) {
            if (isInstalled(lcPkg)) {
                val launch = packageManager.getLaunchIntentForPackage(lcPkg)
                if (launch != null) {
                    Toast.makeText(this, "เปิด Lawnchair แล้วเลือก ชุดไอคอน → พะเยาโฮมคุก ธีม", Toast.LENGTH_LONG).show()
                    startActivity(launch)
                    return
                }
            }
        }
        // No supported launcher installed yet -> friendly 1-tap Play Store link
        AlertDialog.Builder(this)
            .setTitle("เปลี่ยนชุดไอคอนทั้งเครื่อง (36 แบบ)")
            .setMessage("เพื่อให้เปลี่ยนไอคอนทั้งเครื่อง vivo ได้ในคลิกเดียว แนะนำติดตั้งแอปฟรี Nova Launcher จาก Play Store ก่อน แล้วกลับมากดปุ่มนี้อีกครั้งครับ")
            .setPositiveButton("เปิด Play Store (Nova Launcher)") { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.teslacoilsw.launcher")))
                } catch (_: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.teslacoilsw.launcher")))
                }
            }
            .setNegativeButton("ไว้ทีหลัง", null)
            .show()
    }

    private fun isInstalled(pkg: String): Boolean {
        return try {
            packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun saveKeyboardAndChatToGallery() {
        Thread {
            try {
                saveDrawableToPictures(R.drawable.keyboard_background, "phayao_keyboard_bg.png")
                saveDrawableToPictures(R.drawable.wallpaper_chat, "phayao_chat_wallpaper.png")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "บันทึกภาพคีย์บอร์ดและพื้นหลังแชทลงอัลบั้ม Pictures/PhayaoHomeCook แล้ว!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "บันทึกไม่สำเร็จ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun saveDrawableToPictures(resId: Int, displayName: String) {
        val bmp = BitmapFactory.decodeResource(resources, resId)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhayaoHomeCook")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
        }
        bmp.recycle()
    }

    private fun openGboardOrInputSettings() {
        val gboardComponents = listOf(
            android.content.ComponentName(
                "com.google.android.inputmethod.latin",
                "com.google.android.apps.inputmethod.libs.framework.preference.SettingsActivity"
            ),
            android.content.ComponentName(
                "com.google.android.inputmethod.latin",
                "com.android.inputmethod.latin.settings.SettingsActivity"
            )
        )
        for (comp in gboardComponents) {
            try {
                startActivity(Intent().setComponent(comp))
                Toast.makeText(this, "เลือก ธีม → ธีมของฉัน (+) → อัลบั้ม PhayaoHomeCook", Toast.LENGTH_LONG).show()
                return
            } catch (_: Exception) {}
        }
        val gboard = packageManager.getLaunchIntentForPackage("com.google.android.inputmethod.latin")
        if (gboard != null) {
            Toast.makeText(this, "เลือก ธีม → ธีมของฉัน (+) → อัลบั้ม PhayaoHomeCook", Toast.LENGTH_LONG).show()
            startActivity(gboard)
        } else {
            Toast.makeText(this, "แตะที่ Gboard → ธีม → ธีมของฉัน (+) → อัลบั้ม PhayaoHomeCook", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
    }
}
