# Android Device Inspector

A consent-based Android diagnostic app. The phone owner must install, open, review, and choose to share the report.

## What it reports
- Manufacturer, brand, model, Android version, SDK level, security patch, build fingerprint
- Uptime, total/available RAM, internal storage total/free
- Battery percentage and charging status
- Wi-Fi IP, gateway, DNS and DHCP server when Android exposes them

## Privacy
- No Internet permission; the app cannot upload its report.
- No stealth operation, remote control, persistence, or background collection.
- No access to messages, calls, contacts, files, microphone, camera, or location.
- The owner manually chooses whether to share the displayed report.

## Build
1. Open this folder in Android Studio (recent version).
2. Allow Gradle sync and install the Android SDK platform 35 if prompted.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

The Wi-Fi DHCP details can be unavailable on some Android versions or vendor builds. The app does not attempt to bypass Android restrictions.
