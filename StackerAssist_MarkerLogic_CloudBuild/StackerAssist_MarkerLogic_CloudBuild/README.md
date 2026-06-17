# StackerAssist Marker Logic

This version uses your marker-based winning logic.

## Logic implemented

1. Detect the visible stack bands from screen pixels.
2. Pick the current moving band.
3. Pick the placed stack band directly below it.
4. Place an internal marker at the **rightmost x-position** of that placed stack.
5. Sample the marker pixel on the current moving band's height.
6. When that marker pixel changes from background colour into block colour, auto-tap.
7. After the stack is placed, the detector re-detects the new top stack and switches the active marker upward.
8. Repeat.

## Important

- This is a personal/testing Android build.
- It uses normal Android permissions: screen capture, overlay, and Accessibility gestures.
- It does not include root, ADB tapping, stealth behavior, anti-cheat bypassing, overlay bypassing, or screen-capture bypassing.
- Some games may block screen capture or overlays.

## Setup

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run on a real Android phone.
4. Tap **Allow overlay**.
5. Tap **Enable auto tap service**.
6. Enable **Stacker Assist** inside Android Accessibility settings.
7. Return to the app.
8. Adjust settings if needed.
9. Tap **Start screen analysis**.
10. Approve screen capture.
11. Open the stacker game.

## In-app settings

The app now lets you edit:

- Auto tap enabled
- Visual marker enabled
- Block colour saturation threshold
- Block brightness threshold
- Marker X offset
- Marker Y offset
- Marker sample radius
- Marker colour-change threshold
- Tap cooldown
- Tap duration
- Level advance delay
- Tap X/Y ratios
- Screen scan top/bottom ratios
- Minimum band width ratio
- Minimum row pixel ratio
- Sample step
- Minimum vertical gap between moving band and target band
- Motion matching Y distance
- Visual marker Y offset

Press **Save settings** after editing.

## Most important tuning values

### Marker X offset

Default:

```text
0
```

The internal marker is:

```text
target stack right edge + markerXOffsetPx
```

If the app taps too late, try a small negative value like:

```text
-2
```

If it taps too early, try a small positive value like:

```text
2
```

### Marker colour-change threshold

Default:

```text
85
```

Increase it if random UI/background changes trigger taps.

Decrease it if the app misses contact.

### Marker sample radius

Default:

```text
3
```

Increase it if single-pixel noise causes bad detection.

Decrease it if the marker area catches too much surrounding colour.

### Visual marker offset

The visual marker is drawn above the real sample point by default so the overlay does not interfere with pixel detection.

Default:

```text
-42
```

## Main code files

- `MainActivity.kt` — app UI and settings screen.
- `AppSettings.kt` — all editable settings.
- `CaptureService.kt` — screen capture, overlay, detector loop.
- `MarkerStackDetector.kt` — your marker-contact logic.
- `AutoTapAccessibilityService.kt` — performs taps.
- `AutoTapBridge.kt` — sends tap requests from detector to accessibility service.


## Cloud APK build

This ZIP includes a GitHub Actions workflow that builds the APK online.

Read:

```text
CLOUD_BUILD_APK.md
```

Workflow file:

```text
.github/workflows/build-apk.yml
```
