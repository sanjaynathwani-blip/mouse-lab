# Third-party notices

Mouse Lab is under the [GNU AGPL v3.0](LICENSE).

## Ported code

| Component | Used for | License |
| --- | --- | --- |
| [OpenMouse](https://github.com/OpenMouse-Project/openmouse-landing-page) tools (`src/pages/tools`: `polling-math.ts`, `rotation-math.ts`, their tests and the tools' text), commit `c963414` | The polling rate and mouse rotation calculations, ported to Kotlin | AGPL-3.0 |

## Libraries in the app

These open-source libraries are compiled into the APK. All of them are under the
**Apache License 2.0**; its full text is in [`licenses/Apache-2.0.txt`](licenses/Apache-2.0.txt).

| Component | Used for | License |
| --- | --- | --- |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) (runtime, UI, foundation, animation, Material 3) | Drawing the interface | Apache-2.0 |
| [AndroidX](https://developer.android.com/jetpack/androidx) Core, Activity, Lifecycle, Collection, Annotation, SavedState, Startup, Emoji2, ProfileInstaller, Tracing, Graphics, Autofill, Concurrent and their dependencies | The Android app framework | Apache-2.0 |
| [Kotlin standard library](https://github.com/JetBrains/kotlin) | The language runtime | Apache-2.0 |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Refreshing the readings | Apache-2.0 |
| [Guava ListenableFuture](https://github.com/google/guava) (as used by AndroidX) | AndroidX's async results | Apache-2.0 |
| [JetBrains Java Annotations](https://github.com/JetBrains/java-annotations) | Nullability annotations | Apache-2.0 |
