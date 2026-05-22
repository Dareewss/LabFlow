# LabFlow Android Companion

This is the mobile companion app for LabFlow Desktop. The desktop app remains the main system. Android is used for QR scanning and fast field actions.

## Requirements

- Android Studio
- Android device with camera
- Phone and PC on the same Wi-Fi network
- LabFlow Desktop running

## Setup

1. Open `android-companion/` in Android Studio.
2. Let Gradle sync dependencies.
3. Run the app on a physical Android device.
4. Allow camera permission when requested.

## Desktop Connection

LabFlow Desktop starts a local API automatically.

Default values:

```text
Port: 8080
API key: LABFLOW_LOCAL_API_KEY
```

Find the PC IP address on Windows:

```bat
ipconfig
```

Use the IPv4 address of the active Wi-Fi adapter.

## App Flow

1. Enter PC IP.
2. Enter port, usually `8080`.
3. Enter API key.
4. Press `Save configuration`.
5. Press `Test connection`.
6. Press `Scan QR`.
7. Scan a QR code like `LABFLOW-EQ-15`.
8. Equipment details appear on the phone.
9. Press `Raporteaza defect` to create a fault report.

## Implemented Features

- Connection configuration with SharedPreferences
- Health check
- QR scanning with CameraX and ML Kit Barcode Scanning
- Equipment lookup by QR code
- Equipment details view
- Fault report creation
- Retrofit networking with Gson
- API key authorization header

## Prepared API Structure

The app already defines DTOs and Retrofit methods for:

- equipment details
- equipment history
- fault report
- borrow
- return

Borrow and return UI can be expanded safely using the existing request models.

## Troubleshooting

If connection fails:

- Check that LabFlow Desktop is running.
- Check that phone and PC are on the same Wi-Fi.
- Check Windows Firewall for port `8080`.
- Check the PC IP address.
- Check the API key.

If scan does not work:

- Allow camera permission.
- Make sure the QR starts with `LABFLOW-EQ-`.
- Make sure the QR belongs to an equipment item in the desktop database.
