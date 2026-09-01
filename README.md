# LP3-StopWatch

A stopwatch for the **Light Phone 3**, built under the DailyHobbyist name.

- **Package:** `com.dailyhobbyist.stopwatch`
- **Version:** 4.0 (versionCode 4)

## What this repo is

This is a full checkout of the Light Phone SDK. **The app itself lives in `tool/`** —
that's the only folder that holds DailyHobbyist code. Everything else is upstream SDK
scaffolding that has to be present for the build to work.

- `tool/lighttool.toml` — the app manifest, and the **single source of truth for the
  version number**. Both `versionCode` and `versionName` get bumped on every build.
- `tool/` — the app source.

## Building

Built with the Light Phone SDK toolchain and sideloaded onto the device.

`serverPackage` in `tool/lighttool.toml` must be `com.lightos` for a real device. The
value `com.thelightphone.sdk.emulator` is for the SDK emulator only — committing that
value by mistake produces a build that will not run on the phone.

## Related

Other DailyHobbyist Light Phone 3 apps live in sibling repos under `cmg-ops`
(LP3-Lists, LP3-Bible, LP3-Budget, LP3-DrawPad, LP3-Rolodex, LP3-Calculator,
LP3-Routines, and others).
