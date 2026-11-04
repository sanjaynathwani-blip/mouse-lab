# Mouse Lab

Small tools for your mouse, as a native Android app for desktop-mode Android (built and tested on an ASUS Googlebook 14).

- **Polling rate tester**: how many times per second your mouse reports its position (current, average and peak, plus the closest standard rate).
- **Mouse rotation**: swipe side to side with your natural grip and get the sensor rotation angle that levels your swipes, with a margin of error.

These are native ports of the browser tools at [openmouse.app/tools](https://openmouse.app/tools), from the open source [OpenMouse](https://github.com/OpenMouse-Project) project ([openmouse-landing-page](https://github.com/OpenMouse-Project/openmouse-landing-page), `src/pages/tools`). Mouse Lab is not affiliated with OpenMouse.

## Why native

The browser versions are limited by what a page can see. On Android the app can do better:

- **Pointer capture** (`View.requestPointerCapture`) gives raw relative sensor counts with no pointer acceleration, the native form of the browser's pointer lock with `unadjustedMovement`.
- **Per-report timestamps**: every hardware report keeps its own kernel timestamp, including reports Android batches into one `MotionEvent`'s history (`getHistoricalEventTimeNanos`), and `requestUnbufferedDispatch` delivers them as they arrive. So the polling rate isn't capped at the screen refresh rate.

## Use

Click the box to capture the mouse, move it, then right-click or press Escape to stop. A touchpad is not measured; use a USB or Bluetooth mouse.

## Build

Kotlin, Jetpack Compose, minSdk 29, targetSdk 36.

```sh
./gradlew testDebugUnitTest assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

The measuring logic (`app/src/main/java/app/mouselab/tools`) is a line-for-line port of OpenMouse's `polling-math.ts` and `rotation-math.ts`, and its unit tests port the upstream tests. The only change is that the stroke splitter is incremental, so the live swipe count costs O(1) per report.

## License

GNU Affero General Public License v3.0, the same as the OpenMouse code it is ported from. See [LICENSE](LICENSE).

Ported from OpenMouse-Project/openmouse-landing-page at commit `c963414`.
