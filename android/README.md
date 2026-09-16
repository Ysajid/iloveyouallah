# The 99 Names Journey — Android

The native build. Same eleven islands, same 99 names, same bedtime ritual as
the web version — but a real app: Kotlin, Jetpack Compose, installs from an APK,
shows up in the launcher.

## Build it

```
export ANDROID_HOME=/path/to/android-sdk
cd android
gradle :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Copy it to the
tablet and open it — Android will ask once about installing from an unknown
source, then it's a normal app. No Play Store, no account, no sideloading
gymnastics beyond that first prompt.

`gradle :app:assembleRelease` produces a release build signed with the local
debug key, which is fine for installing on your own devices. It is *not* the
key you would use to publish, and the plan says this never goes to a store.

## Where the text lives

Nowhere in this folder. The Bangla is in `../src/names.js`, same as before, and
`node build.js` from the repo root regenerates both the web file and
`app/src/main/assets/names.json`. Edit once, rebuild, both apps change.

Do not hand-edit `names.json` — it gets overwritten on the next build.

## What it's made of

```
app/src/main/java/com/ysajid/names99/
  MainActivity.kt   app state, screen routing, back handling
  Model.kt          names and islands, loaded from the assets JSON
  Progress.kt       who is playing, how far they got — SharedPreferences
  Words.kt          every string that is not one of the 99 names
  Screens.kt        profiles, map, card, finish, the full 99
  Game.kt           the matching game
  Ui.kt             buttons, cards, stars, the "try this today" box
  Theme.kt, Sky.kt  the night sky
  Voice.kt          audio playback, silent until clips exist
```

Four dependencies, all AndroidX: core, activity-compose, Compose, Material 3.
No networking library, no analytics, no crash reporter, nothing to phone home.

`AndroidManifest.xml` asks for no permissions. The built APK shows exactly one
entry, `com.ysajid.names99.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which
AndroidX adds to every app it builds — it is scoped to this package and grants
nothing outside it. The one that matters is absent: there is no `INTERNET`
permission, so the app cannot reach the network even if some future dependency
tried to. Worth re-checking after any dependency change:

```
aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk
```

## Adding your voice later

Drop clips into `app/src/main/assets/audio/` named by the traditional number:

```
app/src/main/assets/audio/
  01.mp3     ← Ar-Rahman
  02.mp3     ← Ar-Raheem
  ...
```

Rebuild. The 🔊 button appears only on cards whose clip is actually there, so
recording ten of them is a perfectly sensible place to stop for the night.

## Notes

- `minSdk 24` — Android 7 and up, which covers any tablet still in daily use.
- Portrait only, edge-to-edge, drawn dark. It gets used in a dark room.
- Progress is per child under `nj99` preferences, keyed by name. Deleting a
  profile deletes that child's progress and nothing else.
- Back from a card, the game, or the list goes to the map. Back from the map
  leaves the app — no accidental exits mid-island.
- The web version in `../dist/` still works and still has its own saved
  progress. The two do not share state; they are two front doors to the same
  content.
