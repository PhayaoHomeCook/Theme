# ธีมมือถือ Android: พะเยาโฮมคุก (Phayao Home Cook)

ชุดธีมสำหรับ Android ครบชุดที่ออกแบบโดยอ้างอิงจากมาสคอต **พะเยาโฮมคุก** (หวดสานประดับลายดอกประจำยามสีทอง + ผ้ากันเปื้อนสีเลือดหมู-ลายผ้าทอไทลื้อ) และโทนสีหลัก **เลือดหมู (`#7A1038`) • ทองล้านนา (`#E3AD52`) • ครีมอบอุ่น (`#FCF5E5`)**

---

## โครงสร้างไฟล์ทั้งหมด

| โฟลเดอร์ / ไฟล์ | รายละเอียด |
|---|---|
| `preview/index.html` | **หน้าพรีวิวธีมแบบโต้ตอบได้** — เปิดดูตัวอย่างหน้าจอล็อก, หน้าหลักโทนครีม/เลือดหมู, แผงตั้งค่าด่วน, แชท+คีย์บอร์ด, คัดลอกรหัสสี และดูไอคอนทั้งหมด |
| `assets/wallpapers/` | วอลเปเปอร์ความละเอียด Full HD (`1080×1920` PNG) ครบ 4 แบบ:<br>• `lockscreen_wallpaper.png` — หน้าจอล็อก (มาสคอต + วิวกว๊านพะเยา + ริบบิ้นผ้าทอล้านนา)<br>• `homescreen_wallpaper_light.png` — หน้าจอหลักโทนครีมอบอุ่น (ไอคอนอ่านง่าย)<br>• `homescreen_wallpaper_dark.png` — หน้าจอหลักโทนเลือดหมูเข้ม<br>• `chat_wallpaper.png` — พื้นหลังห้องแชท (LINE / WhatsApp) |
| `assets/keyboard/keyboard_background.png` | พื้นหลังคีย์บอร์ด `1920×1080` (16:9) สำหรับ Gboard และ Samsung Keys Cafe |
| `assets/icons/` | **ชุดไอคอนเหรียญเลือดหมู-ขอบทองล้านนา 36 แบบ** (`512×512` PNG พื้นใส) พร้อมภาพรวม `icon_sheet_preview.png` และไฟล์ Auto-Mask (`mask/iconback.png`, `mask/iconmask.png`, `mask/iconupon.png`) |
| `assets/source/` | ภาพต้นฉบับที่แนบมา และไฟล์มาสคอตตัดพื้นหลังใส (`mascot_cutout.png`) |
| `palette/` | ชุดรหัสสีครบทั้ง `theme_palette.json` (จับคู่สำหรับ Samsung Theme Park / Keys Cafe), `colors.xml` และ `themes.xml` (Material 3) |
| `android-icon-pack/` | **โปรเจกต์ Android Studio (Kotlin)** พร้อม `appfilter.xml` จับคู่แอปยอดนิยมกว่า 50 แอป + หน้าจอแอปสำหรับกดตั้งวอลเปเปอร์ได้ทันที เมื่อ Build เป็น `.apk` จะใช้เป็น Icon Pack กับ Nova / Lawnchair / Smart Launcher / Samsung Theme Park ได้เลย |
| `scripts/` | สคริปต์สร้างไอคอน (`generate_icons.ps1`) และวอลเปเปอร์ (`generate_wallpapers.ps1`) หากต้องการปรับแต่งสีหรือเพิ่มไอคอนใหม่ในอนาคต |

---

## ชุดสีหลัก (Color Palette)

| ชื่อสี | รหัส Hex | การใช้งาน |
|---|---|---|
| เลือดหมูหลัก (Primary Burgundy) | `#7A1038` | แถบหัวข้อ, พื้นเหรียญไอคอน, ปุ่มหลัก |
| เลือดหมูเข้ม (Dark Plum) | `#4E0822` | Status Bar, พื้นหลัง Dark Mode |
| ชมพูทับทิม (Rose Accent) | `#B9245B` | Badge แจ้งเตือน, บับเบิลแชท, ปุ่มส่ง |
| ทองหวดสาน (Bamboo Gold) | `#E3AD52` | ขอบไอคอน, เส้นคั่น, ไฮไลต์ |
| ทองสว่าง (Gold Highlight) | `#F7DA94` | ลายดอกประจำยาม, ไอคอนสถานะ |
| ครีมอบอุ่น (Warm Cream) | `#FCF5E5` | พื้นหลัง Light Mode, ตัวสัญลักษณ์ไอคอน |
| ครีมทราย (Warm Sand) | `#F3E4CA` | ช่องค้นหา, ปุ่มคีย์บอร์ด |
| น้ำตาลเข้ม (Espresso Text) | `#3D1622` | ข้อความหลักบนพื้นสีครีม |

---

## วิธีนำไปใช้งานบนมือถือ Android

### 1. ใช้ทันทีบน Samsung Galaxy (Good Lock → Theme Park + Keys Cafe)
1. คัดลอกโฟลเดอร์ `assets/` ลงในมือถือ (เช่น ในโฟลเดอร์ `Pictures/PhayaoTheme/`)
2. **ตั้งวอลเปเปอร์:** เลือก `lockscreen_wallpaper.png` สำหรับหน้าจอล็อก และ `homescreen_wallpaper_light.png` หรือ `homescreen_wallpaper_dark.png` สำหรับหน้าจอหลัก
3. **แต่งสีระบบ & แผงควบคุม (Quick Panel):** เปิด **Good Lock → Theme Park → Theme / Quick Panel** แล้วใส่รหัสสีตามตารางใน `palette/theme_palette.json`
4. **ตั้งชุดไอคอน:** ใน **Theme Park → Icon** สามารถกดเปลี่ยนทีละแอปจากโฟลเดอร์ `assets/icons/` (หรือติดตั้ง APK จากโปรเจกต์ `android-icon-pack/` เพื่อเลือกใช้ทั้งแพ็กทีเดียว)
5. **แต่งคีย์บอร์ด:** เปิด **Theme Park → Keyboard** (หรือ **Keys Cafe**) เลือกภาพพื้นหลัง `assets/keyboard/keyboard_background.png` และตั้งสีปุ่ม `#FFFBF2`, ตัวอักษร `#4E0822`, ปุ่ม Enter `#B9245B`

### 2. ใช้บนมือถือ Android ทุกรุ่น (Nova Launcher / Lawnchair / Smart Launcher)
1. ตั้งวอลเปเปอร์จาก `assets/wallpapers/`
2. เปลี่ยนไอคอนแต่ละแอปด้วยการแตะค้าง → แก้ไข (Edit) → เลือกรูปจาก `assets/icons/`
3. **หรือ Build เป็นแอป Icon Pack (.apk) ทีเดียวจบ:**
   - เปิดโฟลเดอร์ `android-icon-pack/` ใน **Android Studio**
   - กด **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - ติดตั้งไฟล์ `app-debug.apk` บนมือถือ แล้วเลือก **พะเยาโฮมคุก ธีม** ในเมนู Icon Pack ของ Launcher (ระบบจะเปลี่ยนไอคอนอัตโนมัติพร้อมใส่กรอบเหรียญเลือดหมู-ทองให้แอปอื่นๆ ด้วย `iconback/iconmask/iconupon`)

### 3. ตั้งคีย์บอร์ด Gboard & พื้นหลังแชท LINE
- **Gboard:** การตั้งค่า Gboard → ธีม → ธีมของฉัน (`+`) → เลือก `assets/keyboard/keyboard_background.png` (แนะนำปรับความสว่างภาพประมาณ 80–90% และเปิดขอบปุ่ม)
- **LINE:** เปิดห้องแชทหรือตั้งค่าแชท → วอลเปเปอร์ → เลือกรูป `assets/wallpapers/chat_wallpaper.png`
