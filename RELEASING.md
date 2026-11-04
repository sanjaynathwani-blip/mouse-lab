# Releasing Mouse Lab

A release goes out as a **pre-release** first, its APK is checked on a real device, and only then
does it become the latest release that the README's download link points to.

## 1. Prepare

1. In `app/build.gradle.kts`, bump `versionCode` by one and set `versionName`.
2. In `CHANGELOG.md`, rename **Unreleased** to the version and date.
3. Commit as "Version X.Y.Z" and push.

## 2. Build

The release key lives in `~/.config/mouse-lab/` (never in the repo); it is the same key that signs
Perfect Sound. Every release must be signed with it, or Android refuses to update over earlier
installs.

```sh
./gradlew testDebugUnitTest assembleRelease
mkdir -p build/release && cp app/build/outputs/apk/release/app-release.apk build/release/MouseLab.apk
(cd build/release && sha256sum MouseLab.apk > SHA256SUMS)

# Same signing certificate as the current release? (SHA-256 89:81:0A:22...)
apksigner verify --print-certs build/release/MouseLab.apk | grep SHA-256
```

## 3. Publish as a pre-release

```sh
git tag vX.Y.Z && git push origin vX.Y.Z
gh release create vX.Y.Z build/release/MouseLab.apk build/release/SHA256SUMS --prerelease --title "Mouse Lab X.Y.Z" --notes-file build/release/notes.md
```

## 4. Check the published APK on a device

Mouse Lab asks for no permissions, so there are no restricted settings to trip over; what matters
is that the file people download installs and works.

1. Download the pre-release's **MouseLab.apk** and compare it with `SHA256SUMS`.
2. Install it over the previous release (this checks the signature), open Mouse Lab and measure:
   a mouse at a known rate reads that rate, and swipes at a known angle give that angle
   (`tools/vmouse.py` can be the mouse).

## 5. Make it the latest release

```sh
gh release edit vX.Y.Z --prerelease=false --latest
```

If the check fails, fix it, bump the version again and start over; don't replace a published APK.
