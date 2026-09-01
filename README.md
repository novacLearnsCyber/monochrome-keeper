i vibe coded this whole app , it works on any phone with android>8 oreo 


what u need : phone , usb type c , adb , pc/laptop with windows , linux , macos.
i runned this setup on my kali 

1.enable developer mode , usually something like this 
Settings → About phone → Build number (press 7 times)

> A message **"You are now a developer!"** will appear , put in password or pin if needed.
---


2.enable usb debugging

Settings -> Developer options -> USB debugging -> Enable it -> press ok 

for Xiaomi / MIUI:
- also enable **"Install via USB"** 
- enable **"USB debugging (Security settings)"** —  may be asked for a Mi account and 7 days of waiting.

---

3.phone connect
connect the phone to pc with the usb -> phone notification — select: - **"File Transfer" / "MTP"** (no "Charging only")
-> dialog box **"Allow USB debugging?"**:
   - select **"Always allow from this computer"**
   - select **Allow**

if the dialog box does not appear : take out the cable -> disable usb debugging -> reenable -> put the cable back on 

install ADB
 Linux (Debian / Ubuntu / Kali)

```bash
sudo apt update
sudo apt install -y adb
```

manual download
```bash
mkdir -p ~/Android/Sdk
cd /tmp
wget https://dl.google.com/android/repository/platform-tools-latest-linux.zip
unzip platform-tools-latest-linux.zip
mv platform-tools ~/Android/Sdk/
echo 'export PATH=$PATH:$HOME/Android/Sdk/platform-tools' >> ~/.bashrc
source ~/.bashrc
```

macOS

```bash
# Cu Homebrew
brew install android-platform-tools
```

manual download: https://developer.android.com/tools/releases/platform-tools

Windows

1. download: https://developer.android.com/tools/releases/platform-tools
2. extract the zip 
3. open cmd in that folder



check connection: 

```bash
adb devices
```

u should see :
```
List of devices attached
XXXXXXXXX    device
```

what u may see 

| Output | Problem | FIX |
|--------|----------|---------|
| `XXXX device` |  all ok | 
| `XXXX unauthorized` | phone did not authorized the pc  | press allow |
| `XXXX offline` | non stable connection | change the usb cable |
| *(gol)* | phone not detected | check the cable ,usb debugginh , drivers |

if not detected (Windows):

install vendor drivers
- **Samsung**: https://developer.samsung.com/android-usb-driver
- **Google/Universal**: Inclus în platform-tools
- **Xiaomi**: https://developer.xiaomi.com/
- for any other brands ust search on google for the drivers

---

4. install the app 

Option A — APK Pre-compiled

```bash

adb install app-debug.apk

adb shell pm grant com.monochrome.keeper android.permission.WRITE_SECURE_SETTINGS
```

Option B — Compile from source 


```bash
cd yourpath

gradle wrapper --gradle-version 8.5

./gradlew assembleDebug

adb install app/build/outputs/apk/debug/app-debug.apk

adb shell pm grant com.monochrome.keeper android.permission.WRITE_SECURE_SETTINGS
```

---

5.permissions

```bash
adb shell pm grant com.monochrome.keeper android.permission.WRITE_SECURE_SETTINGS
```

should be run only one time 

should run find rn 

to uninstall it 

```bash
adb shell settings put secure accessibility_display_daltonizer_enabled 0
adb shell settings put secure accessibility_display_daltonizer -1

adb uninstall com.monochrome.keeper
```

hope someone find this usefull
