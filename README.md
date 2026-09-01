# 📱 Monochrome Keeper — Tutorial Instalare (Orice Telefon Android)

Aplicație care menține automat modul **monochrome (grayscale)** activ. Verifică la fiecare **15 minute** — dacă e dezactivat, îl reactivează automat.

> [!IMPORTANT]
> **Cerință minimă:** Android 8.0 (Oreo) sau mai nou. Funcționează pe orice marcă: Samsung, Xiaomi, OnePlus, Pixel, Huawei, Oppo, Realme, Motorola, etc.

---

## 📋 Ce Ai Nevoie

| Element | Detalii |
|---------|---------|
| Telefon Android | Versiune 8.0+ (Oreo sau mai nou) |
| Cablu USB | USB-C sau Micro-USB (care vine cu telefonul) |
| PC/Laptop | Linux, macOS sau Windows |
| ADB | Se instalează gratuit (instrucțiuni mai jos) |

---

## Partea 1: Pregătește Telefonul

### 🔓 Pasul 1 — Activează Developer Options

Trebuie să activezi meniul ascuns de developer. Procesul diferă puțin pe fiecare marcă:

#### Samsung (Galaxy A54, S24, etc.)
```
Settings → About phone → Software information → Build number (apasă de 7 ori)
```

#### Xiaomi / Redmi / POCO
```
Settings → About phone → MIUI version (apasă de 7 ori)
```

#### OnePlus
```
Settings → About device → Build number (apasă de 7 ori)
```

#### Google Pixel
```
Settings → About phone → Build number (apasă de 7 ori)
```

#### Huawei / Honor
```
Settings → About phone → Build number (apasă de 7 ori)
```

#### Oppo / Realme
```
Settings → About phone → Build number (apasă de 7 ori)
```

> [!TIP]
> Va apărea mesajul **"You are now a developer!"** sau **"Modul dezvoltator a fost activat!"**. Dacă ți se cere PIN/parolă, introdu-l.

---

### 🔧 Pasul 2 — Activează USB Debugging

1. Mergi la **Settings → Developer options** (sau **Setări → Opțiuni pentru dezvoltatori**)
2. Caută **USB debugging** (Depanare USB)
3. **Activează-l** (toggle ON)
4. Confirmă cu **OK** pe dialogul de avertizare

#### Extra pentru Xiaomi / MIUI:
- Activează și **"Install via USB"** (Instalare prin USB)
- Activează **"USB debugging (Security settings)"** — poate necesita cont Mi și 7 zile de așteptare pe MIUI mai vechi

---

### 🔌 Pasul 3 — Conectează Telefonul la PC

1. Conectează telefonul la PC prin **cablu USB**
2. Pe telefon apare o notificare USB — selectează:
   - **"File Transfer" / "MTP"** (NU "Charging only")
3. Va apărea un dialog **"Allow USB debugging?"**:
   - ✅ Bifează **"Always allow from this computer"**
   - Apasă **Allow**

> [!WARNING]
> Dacă dialogul NU apare: deconectează cablul → dezactivează USB debugging → reactivează-l → reconectează cablul.

---

## Partea 2: Instalează ADB pe PC

### 🐧 Linux (Debian / Ubuntu / Kali)

```bash
sudo apt update
sudo apt install -y adb
```

Sau descarcă manual:
```bash
mkdir -p ~/Android/Sdk
cd /tmp
wget https://dl.google.com/android/repository/platform-tools-latest-linux.zip
unzip platform-tools-latest-linux.zip
mv platform-tools ~/Android/Sdk/
echo 'export PATH=$PATH:$HOME/Android/Sdk/platform-tools' >> ~/.bashrc
source ~/.bashrc
```

### 🍎 macOS

```bash
# Cu Homebrew
brew install android-platform-tools
```

Sau descarcă manual de la: https://developer.android.com/tools/releases/platform-tools

### 🪟 Windows

1. Descarcă: https://developer.android.com/tools/releases/platform-tools
2. Extrage ZIP-ul într-un folder (ex: `C:\platform-tools`)
3. Deschide **Command Prompt** în acel folder
4. Sau adaugă folderul la PATH din System Environment Variables

### ✅ Verifică instalarea

```bash
adb version
```

Trebuie să vezi: `Android Debug Bridge version 1.0.xx`

---

## Partea 3: Verifică Conexiunea

```bash
adb devices
```

Trebuie să vezi:
```
List of devices attached
XXXXXXXXX    device
```

**Ce poate apărea și ce să faci:**

| Output | Problemă | Soluție |
|--------|----------|---------|
| `XXXX device` | ✅ Totul OK | Continuă la pasul următor |
| `XXXX unauthorized` | Telefonul n-a autorizat PC-ul | Verifică dialogul pe telefon, apasă Allow |
| `XXXX offline` | Conexiune instabilă | Schimbă cablul USB, reconectează |
| *(gol)* | Telefonul nu e detectat | Verifică cablul, USB debugging, drivere |

### Dacă nu e detectat (Windows):

Instalează driverele USB ale producătorului:
- **Samsung**: https://developer.samsung.com/android-usb-driver
- **Google/Universal**: Inclus în platform-tools
- **Xiaomi**: https://developer.xiaomi.com/
- **Alte mărci**: Caută pe Google "[marca] USB driver ADB"

---

## Partea 4: Instalează Aplicația

### Opțiunea A — APK Pre-compilat (Cel mai simplu)

Dacă ai deja fișierul `app-debug.apk`:

```bash
# Instalează pe telefon
adb install app-debug.apk

# Acordă permisiunea specială (OBLIGATORIU!)
adb shell pm grant com.monochrome.keeper android.permission.WRITE_SECURE_SETTINGS
```

### Opțiunea B — Compilare din Sursă

Dacă ai codul sursă și Java instalat:

```bash
cd /calea/catre/androidAPP

# Prima dată — generează wrapper
gradle wrapper --gradle-version 8.5

# Compilează
./gradlew assembleDebug

# Instalează
adb install app/build/outputs/apk/debug/app-debug.apk

# Acordă permisiunea
adb shell pm grant com.monochrome.keeper android.permission.WRITE_SECURE_SETTINGS
```

---

## Partea 5: Acordă Permisiunea (CRITIC!)

Această comandă e **obligatorie** — fără ea aplicația nu poate activa modul monochrome:

```bash
adb shell pm grant com.monochrome.keeper android.permission.WRITE_SECURE_SETTINGS
```

> [!CAUTION]
> **Trebuie rulată O SINGURĂ DATĂ.** Permisiunea rămâne acordată permanent, chiar și după restart. Trebuie re-rulată DOAR dacă dezinstalezi complet aplicația și o reinstalezi.

**Verificare — testează de pe PC că permisiunea funcționează:**
```bash
# Activează monochrome manual ca test
adb shell settings put secure accessibility_display_daltonizer_enabled 1
adb shell settings put secure accessibility_display_daltonizer 0

# Ecranul telefonului ar trebui să devină grayscale!

# Dezactivează (ca test)
adb shell settings put secure accessibility_display_daltonizer_enabled 0
```

---

## Partea 6: Folosește Aplicația

1. **Găsește** aplicația **"Monochrome Keeper"** pe telefon (în lista de aplicații)
2. **Deschide-o**
3. Vei vedea:
   - 🟢 **Card verde** = Monochrome e ACTIV
   - 🔴 **Card roșu** = Monochrome e DEZACTIVAT (va fi reactivat automat)
4. **Butoane disponibile:**
   - **🔍 Verifică Acum** — forțează o verificare instant
   - **⏹ Oprește Serviciul** — dezactivează verificarea automată
   - **▶ Pornește Serviciul** — reactivează verificarea automată

### Funcționare Automată:
- ✅ Verifică **la fiecare 15 minute** în background
- ✅ Funcționează cu aplicația **închisă**
- ✅ Se repornește automat după **restart telefon**
- ✅ **Fără root** necesar
- ✅ Consum minim de baterie (folosește WorkManager)

---

## 🔧 Troubleshooting

### "adb: command not found"
ADB nu e în PATH. Folosește calea completă:
```bash
# Linux — dacă ai instalat SDK-ul manual
~/Android/Sdk/platform-tools/adb devices

# Sau reinstalează
sudo apt install -y adb
```

### "error: no devices/emulators found"
- Verifică că USB debugging e **activat** pe telefon
- Încearcă alt **cablu USB** (unele cabluri sunt doar de încărcare)
- Selectează **"File Transfer"** în notificarea USB
- Restartează ADB:
```bash
adb kill-server
adb start-server
adb devices
```

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
Există o versiune veche instalată. Dezinstaleaz-o întâi:
```bash
adb uninstall com.monochrome.keeper
adb install app-debug.apk
```

### "INSTALL_FAILED_USER_RESTRICTED" (Xiaomi)
Activează **"Install via USB"** din Developer Options pe telefon.

### Aplicația dă eroare SecurityException
Permisiunea nu a fost acordată. Rulează din nou:
```bash
adb shell pm grant com.monochrome.keeper android.permission.WRITE_SECURE_SETTINGS
```

### Monochrome nu se activează
Testează manual din ADB:
```bash
adb shell settings put secure accessibility_display_daltonizer_enabled 1
adb shell settings put secure accessibility_display_daltonizer 0
```
Dacă nici manual nu merge, telefonul tău poate avea o implementare diferită a setării.

### Verificarea nu rulează exact la 15 minute
E normal. Android **WorkManager** optimizează consumul de baterie și poate rula task-ul cu un decalaj de câteva minute. Funcționalitatea e garantată, dar timing-ul exact poate varia.

---

## ❓ Întrebări Frecvente

**Q: Trebuie să țin telefonul conectat la PC?**
> Nu. Conectarea la PC e necesară DOAR pentru instalare și acordarea permisiunii. După aceea, deconectezi cablul și aplicația funcționează independent.

**Q: Consumă multă baterie?**
> Nu. Folosește WorkManager care e optimizat de Android. Verificarea la 15 minute e foarte eficientă energetic.

**Q: Funcționează după restart?**
> Da. Aplicația are un BootReceiver care repornește automat serviciul de verificare.

**Q: Pot dezinstala aplicația normal?**
> Da, ca orice aplicație: lung-press pe icon → Uninstall, sau din Settings → Apps.

**Q: Ce face exact monochrome/grayscale?**
> Transformă ecranul în alb-negru. E util pentru reducerea dependenței de telefon, pentru focus, sau pentru economisirea bateriei pe ecrane AMOLED.

**Q: Funcționează pe tabletă?**
> Da, pe orice dispozitiv cu Android 8.0+.

---

## 🔓 Dezinstalare Completă

Dacă vrei să elimini aplicația și să resetezi totul:

```bash
# Dezactivează monochrome
adb shell settings put secure accessibility_display_daltonizer_enabled 0
adb shell settings put secure accessibility_display_daltonizer -1

# Dezinstalează aplicația
adb uninstall com.monochrome.keeper
```

Sau simplu de pe telefon: **Settings → Apps → Monochrome Keeper → Uninstall**
