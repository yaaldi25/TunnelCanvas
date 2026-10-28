# TunnelCanvas (starter)

A Kotlin + Jetpack Compose Android starter for:
- Dark/purple UI
- In-memory profile manager
- Import/export of the app's own JSON profile format
- Image selection, preview, grayscale filter, and PNG-to-Base64 embedding

## Important scope notes

This is a **starter source project**, not a VPN client yet. The dashboard explicitly indicates that it does not establish VPN connections. It does not integrate a tunnel core, `VpnService`, SSH, TLS, Trojan, VLESS, VMess, or other live protocols.

The JSON format is TunnelCanvas's own metadata wrapper. Exported JSON is **not** directly a valid configuration for sing-box, Mihomo, or another tunnel engine. A future implementation should add explicit import/export adapters per supported engine and protocol.

Profile records are kept in memory and are lost when the process is closed. Persistent storage, image size limits, crop controls, and robust parser validation should be added before production.

## Requirements

- Android Studio with Android SDK Platform 35 installed
- JDK 17
- Gradle wrapper 8.9 (Android Gradle Plugin 8.7.3)

## Build

Open the `TunnelCanvas` folder in Android Studio and allow Gradle sync to complete. Then use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Or from a terminal in the project root, with JDK 17 and Android SDK configured:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

Expected debug APK path after a successful build:

`app/build/outputs/apk/debug/app-debug.apk`

The APK is not included in this source archive; it must be built locally.

## Manual test checklist

1. Launch the app and check that Home, Profiles, and Image Lab open.
2. In Profiles, save a profile with a sample hostname and port. Confirm it appears in the list.
3. In Image Lab, choose a small PNG/JPEG image and verify the preview appears.
4. Toggle Grayscale and verify the preview changes.
5. Export the profile with image as JSON.
6. Re-import that JSON from Home or Profiles. Verify name/server/port and image are restored.
7. Try importing an unrelated JSON file; the app should show the expected-format error.
8. Test a large image and malformed input before expanding the app; production builds should add size limits and better error handling.
