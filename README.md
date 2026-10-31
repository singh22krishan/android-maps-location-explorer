# Android Maps Location Explorer

A Kotlin Android portfolio app for exploring Ontario destinations on Google Maps while optionally showing the user's current device location.

This repository consolidates the strongest pieces from two map/location projects I developed, while removing embedded API credentials and unnecessary location logging.

## Features

- Google Maps rendered with Maps Compose
- Selectable Ontario destinations
- Runtime fine-location permission flow
- Current location from Fused Location Provider
- Source and destination markers
- Camera animation to fit both points
- Quick controls to re-center on the user or selected destination
- Map click feedback without writing exact coordinates to logs

## Tech Stack

Kotlin · Jetpack Compose · Material 3 · Google Maps Compose · Google Play Services Location

## Maps API Key

No Google Maps API key is committed to this repository.

Provide the key as either a Gradle property or environment variable:

```text
MAPS_API_KEY=your_key_here
```

The build reads the value into an Android manifest placeholder. Restrict the key in Google Cloud to the Android application package/signing certificate and only the Maps SDK APIs required by this app.

## Run

1. Open the repository in Android Studio.
2. Configure `MAPS_API_KEY` outside source control.
3. Sync Gradle.
4. Run on an emulator/device with Google Play services (API 29+).
5. Grant location permission when prompted if you want the source-location marker.

## Security / Privacy

- No API key is stored in source control.
- Exact device coordinates are not written to Logcat.
- Location is requested only after a destination is selected and permission is granted.
- The application does not persist device location.

## Portfolio Note

This is a cleaned standalone portfolio consolidation of Android map/location work I developed. The original academic projects remain separate from this public version.
