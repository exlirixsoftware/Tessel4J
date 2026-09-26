# lib/

Build-time dependency for `tessel-laf`'s native Windows chrome (see [`NativeWindowEffects`](../tessel-laf/src/main/java/net/tessel/laf/win/NativeWindowEffects.java)): drop shadow, Windows 11 rounded corners, dark-mode-aware system menu, and native drag/resize/Snap Layouts hit-testing on `TesselFrame`.

Download both jars from Maven Central and place them here:

| File | Maven coordinates |
| --- | --- |
| `jna-<version>.jar` | `net.java.dev.jna:jna` |
| `jna-platform-<version>.jar` | `net.java.dev.jna:jna-platform` |

- <https://central.sonatype.com/artifact/net.java.dev.jna/jna>
- <https://central.sonatype.com/artifact/net.java.dev.jna/jna-platform>

`build.ps1` / `build.sh` pick up whatever versions are here (matched by filename prefix), compile `tessel-laf` against them, and merge their classes - including JNA's bundled native dispatch library - into the shipped `tessel-laf-<version>.jar`, so consumers of that jar don't need these files themselves. Only this build needs them locally.

Not used on macOS/Linux at runtime (`NativeWindowEffects.install` no-ops off Windows), but still required at compile time since `TesselFrame` references the API.
