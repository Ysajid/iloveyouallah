# The 99 Names Journey

A small app for my kids. Eleven islands, nine names each, one island a night.

Not a product. No store, no accounts, no internet after the first open.

---

## The ritual it's built for

One island a night, eleven nights.

1. Nine cards, then the matching game.
2. The island lights up.
3. I read the **আজকে এটা করো** line at bedtime.
4. Ask about it in the morning.

After the eleven nights the islands stay open to go back to, and the full
ordered list is there for looking things up.

---

## Where it stands

| Step | State |
| --- | --- |
| Bangla pass — 99 × 3 lines | **done**, first pass, needs my read-through |
| Progress sticks (localStorage) | **done** |
| Home screen / icon / manifest | **done** |
| Profiles (one tablet, separate journeys) | **done** |
| My voice on the names | **hooks in, clips not recorded** |

Decisions that were open in the plan, now settled:

- **Ages** — written for the six-year-old. Short sentences, plain words, one
  concrete thing to do. The ten-year-old can read it without it feeling babyish.
- **Language** — Bangla is the default, with a `বাংলা / EN` toggle in the corner.
  The English is a real translation, not a placeholder, so it still works when
  they're older.
- **Audio** — the playback is built and hidden. Drop the clips in and it appears.

---

## Getting it onto their device

```
node build.js
```

That writes **`dist/99-names.html`** — one file, no dependencies, nothing to
install. Then:

1. Copy `dist/99-names.html` to the tablet.
2. Open it once in Chrome or Safari.
3. **Add to Home Screen.**

It launches full-screen with no address bar, and needs no connection after that.
The icon and web manifest are baked into the file as data URIs, so there are no
loose files to keep next to it.

One practical note: browsers are stricter about `file://` pages than about
served ones, and on some Android builds "Add to Home Screen" from a local file
makes a plain bookmark instead of a full-screen app. If that happens, put the
file in any folder you can serve over http on the home wifi — `python3 -m
http.server` in that folder is enough — open it once from that address and add
it from there. After the first open it is cached and offline either way.

---

## Editing the Bangla

All the text is in **`src/names.js`**, one entry per name:

```js
{ n: 1, i: 1, ar: "الرَّحْمَٰنُ", tr: "Ar-Rahmān",
  bn: { meaning: "সবচেয়ে বেশি দয়ালু",
        about: "আল্লাহর দয়া সবার জন্য। ভালো মানুষ, দুষ্টু মানুষ, পাখি, পিঁপড়া — সবার জন্য।",
        today: "আজকে একটা পাখি বা পিঁপড়াকে একটু খাবার দাও।" },
  en: { ... } },
```

- `n` is the position in the traditional order (1–99) — that's what the full list uses.
- `i` is which island it sits on (1–11).
- `ar` and `tr` stay exactly as they are.
- `meaning` / `about` / `today` are the three lines to rewrite.

Change the text, run `node build.js`, copy the file across again. Island names
live in `src/islands.js`.

The `today` lines are the ones that carry the tone. If one sounds like a school
textbook rather than me talking at bedtime, it's wrong — rewrite it.

---

## Adding your voice later

The card looks for `audio/NN.mp3` next to the HTML file, where `NN` is the
**traditional number** of the name, zero-padded to two digits:

```
99-names.html
audio/
  01.mp3     ← Ar-Rahman
  02.mp3     ← Ar-Raheem
  ...
  99.mp3     ← As-Sabur
```

The 🔊 button only appears when the clip for that card actually loads, so
partial recording is fine — record ten names, and those ten cards get a button
while the rest stay as they are. No code change needed.

(Recording all 99 is roughly forty minutes on a phone. The plan says do it last,
once everything else works. It does work now.)

---

## How it's put together

```
src/names.js       the 99 names — the file to edit
src/islands.js     the eleven islands
src/app.js         screens, unlocking, stars, the game, storage
src/styles.css     styling
src/template.html  the shell
build.js           inlines all of the above into one file + draws the icon
dist/99-names.html the built app (committed, so it's always there to copy)
```

The build has no npm dependencies. `build.js` writes the PNG icons itself so
there's nothing to install — plain `node build.js` on any machine with Node.

**Progress** is kept in `localStorage` under `nj99.*` keys, one entry per child.
If storage is blocked (private browsing), the app still runs and says so on the
first screen instead of silently losing the nights.

**Stars**, per island: one for reading all nine cards, two for finishing the
game, three for finishing it without a wrong match. An island unlocks when the
one before it is finished.

---

## Deliberately left out

- Scholarly review. Worth doing if this ever goes further than my kids.
- Translations beyond Bangla and English.
- Analytics, streaks, notifications. They don't need nagging from a tablet.
