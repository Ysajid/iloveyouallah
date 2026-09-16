/* ---------------------------------------------------------------
   build.js — turns src/ into both builds of the app.

       node build.js

   Outputs:
     dist/99-names.html                        the standalone web app
     android/app/src/main/assets/names.json    data for the native app

   src/names.js is the single source of the Bangla. Edit it once and
   both the web file and the Android app pick the change up.
   --------------------------------------------------------------- */

const fs = require("fs");
const path = require("path");
const zlib = require("zlib");

const SRC = path.join(__dirname, "src");
const OUT = path.join(__dirname, "dist");
const OUT_FILE = path.join(OUT, "99-names.html");

/* ---------------- a very small PNG writer ----------------
   Only needs to draw the app icon, so: 8-bit RGBA, no interlacing.
   Written by hand because the build should not need npm install. */

const CRC_TABLE = (() => {
  const table = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c;
  }
  return table;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const body = Buffer.concat([Buffer.from(type, "ascii"), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body), 0);
  return Buffer.concat([len, body, crc]);
}

function encodePng(width, height, rgba) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;    // bit depth
  ihdr[9] = 6;    // truecolour with alpha
  ihdr[10] = 0;   // deflate
  ihdr[11] = 0;   // adaptive filtering
  ihdr[12] = 0;   // no interlace

  // one filter byte (0 = none) in front of every scanline
  const raw = Buffer.alloc(height * (1 + width * 4));
  for (let y = 0; y < height; y++) {
    const from = y * width * 4;
    const to = y * (1 + width * 4);
    raw[to] = 0;
    rgba.copy(raw, to + 1, from, from + width * 4);
  }

  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", ihdr),
    chunk("IDAT", zlib.deflateSync(raw, { level: 9 })),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

/* ---------------- the icon: a gold crescent over a night sky ---------------- */

const SKY_TOP    = [29, 18, 64];
const SKY_BOTTOM = [11, 16, 38];
const GOLD       = [244, 201, 93];

// fixed sprinkle so every build produces an identical icon
const ICON_STARS = [
  [0.17, 0.20, 0.028], [0.82, 0.16, 0.022], [0.26, 0.76, 0.020],
  [0.75, 0.80, 0.026], [0.12, 0.52, 0.016], [0.88, 0.47, 0.018],
  [0.35, 0.12, 0.015], [0.62, 0.88, 0.016],
];

function mix(a, b, k) {
  return [
    Math.round(a[0] + (b[0] - a[0]) * k),
    Math.round(a[1] + (b[1] - a[1]) * k),
    Math.round(a[2] + (b[2] - a[2]) * k),
  ];
}

/* Coverage of one pixel, sampled on a 3x3 grid so the curves are not jagged. */
function coverage(px, py, size, test) {
  let hits = 0;
  for (let sy = 0; sy < 3; sy++) {
    for (let sx = 0; sx < 3; sx++) {
      const x = (px + (sx + 0.5) / 3) / size;
      const y = (py + (sy + 0.5) / 3) / size;
      if (test(x, y)) hits++;
    }
  }
  return hits / 9;
}

/**
 * opts.scale       shrink the design towards the middle (1 = fills the tile)
 * opts.background  false leaves the sky out and the pixels transparent
 * opts.circle      true clips to a circle, for the round launcher icon
 */
function drawIcon(size, opts = {}) {
  const scale = opts.scale === undefined ? 1 : opts.scale;
  const withSky = opts.background !== false;
  const circle = opts.circle === true;

  const rgba = Buffer.alloc(size * size * 4);

  const cx = 0.5 + (0.46 - 0.5) * scale;
  const cy = 0.5 + (0.53 - 0.5) * scale;
  const R = 0.30 * scale;
  const IR = R * 0.90;
  const ix = cx + R * 0.598;
  const iy = cy - R * 0.283;

  const inCrescent = (x, y) => {
    const d1 = (x - cx) ** 2 + (y - cy) ** 2;
    const d2 = (x - ix) ** 2 + (y - iy) ** 2;
    return d1 <= R * R && d2 > IR * IR;
  };

  const stars = ICON_STARS.map(([sx, sy, sr]) => [
    0.5 + (sx - 0.5) * scale,
    0.5 + (sy - 0.5) * scale,
    sr * scale,
  ]);
  const inStars = (x, y) => stars.some(([sx, sy, sr]) =>
    (x - sx) ** 2 + (y - sy) ** 2 <= sr * sr);

  for (let py = 0; py < size; py++) {
    for (let px = 0; px < size; px++) {
      const xx = (px + 0.5) / size;
      const yy = (py + 0.5) / size;

      const moon = coverage(px, py, size, inCrescent);
      const star = coverage(px, py, size, inStars);

      let col;
      let alpha;

      if (withSky) {
        col = mix(SKY_TOP, SKY_BOTTOM, yy);
        const gx = xx - cx, gy = yy - cy;
        const glow = Math.max(0, 1 - Math.sqrt(gx * gx + gy * gy) / (0.62 * scale));
        col = mix(col, [58, 44, 110], glow * glow * 0.55);
        if (moon > 0) col = mix(col, GOLD, moon);
        if (star > 0) col = mix(col, [255, 252, 240], star * 0.9);
        alpha = 255;
      } else {
        // foreground only: the moon and the stars, nothing behind them
        const ink = Math.max(moon, star);
        col = star > moon ? [255, 252, 240] : GOLD;
        alpha = Math.round(ink * 255);
      }

      if (circle) {
        const dx = xx - 0.5, dy = yy - 0.5;
        const edge = coverage(px, py, size, (x, y) =>
          (x - 0.5) ** 2 + (y - 0.5) ** 2 <= 0.5 * 0.5);
        alpha = Math.round(alpha * edge);
      }

      const off = (py * size + px) * 4;
      rgba[off] = col[0];
      rgba[off + 1] = col[1];
      rgba[off + 2] = col[2];
      rgba[off + 3] = alpha;
    }
  }

  return encodePng(size, size, rgba);
}

function dataUri(png) {
  return "data:image/png;base64," + png.toString("base64");
}

/* ---------------- put it all together ---------------- */

function read(name) {
  return fs.readFileSync(path.join(SRC, name), "utf8");
}

function build() {
  const icon192 = dataUri(drawIcon(192));
  const icon512 = dataUri(drawIcon(512));
  const icon180 = dataUri(drawIcon(180));

  const manifest = {
    name: "৯৯ নামের যাত্রা",
    short_name: "৯৯ নাম",
    description: "Eleven islands, nine names each, one island a night.",
    start_url: ".",
    scope: ".",
    display: "standalone",
    orientation: "portrait",
    background_color: "#0b1026",
    theme_color: "#0b1026",
    lang: "bn",
    icons: [
      { src: icon192, sizes: "192x192", type: "image/png", purpose: "any" },
      { src: icon512, sizes: "512x512", type: "image/png", purpose: "any" },
      { src: icon512, sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };

  const manifestUri =
    "data:application/manifest+json," + encodeURIComponent(JSON.stringify(manifest));

  const html = read("template.html")
    .replace("{{STYLES}}", () => read("styles.css").trim())
    .replace("{{ISLANDS}}", () => read("islands.js").trim())
    .replace("{{NAMES}}", () => read("names.js").trim())
    .replace("{{APP}}", () => read("app.js").trim())
    .replace("{{MANIFEST}}", () => manifestUri)
    .replace(/\{\{ICON180\}\}/g, () => icon180);

  const leftover = html.match(/\{\{[A-Z0-9]+\}\}/g);
  if (leftover) throw new Error("template placeholder not filled: " + leftover.join(", "));

  fs.mkdirSync(OUT, { recursive: true });
  fs.writeFileSync(OUT_FILE, html, "utf8");

  const kb = (Buffer.byteLength(html, "utf8") / 1024).toFixed(0);
  console.log("built dist/99-names.html  (" + kb + " KB, one file, no dependencies)");

  writeAndroidData();
  writeAndroidIcons();
}

/* The native app reads the same names out of an asset, so the Bangla
   only ever lives in one place. */
function writeAndroidData() {
  const scope = {};
  new Function("s", "with (s) { " + read("islands.js") + "\n" + read("names.js") +
    "\n s.ISLANDS = ISLANDS; s.NAMES = NAMES; }")(scope);

  if (scope.NAMES.length !== 99) {
    throw new Error("expected 99 names, found " + scope.NAMES.length);
  }
  const perIsland = {};
  scope.NAMES.forEach((x) => { perIsland[x.i] = (perIsland[x.i] || 0) + 1; });
  const wrong = scope.ISLANDS.filter((isl) => perIsland[isl.i] !== 9);
  if (wrong.length) {
    throw new Error("islands without exactly nine names: " +
      wrong.map((i) => i.i).join(", "));
  }

  const assets = path.join(__dirname, "android", "app", "src", "main", "assets");
  fs.mkdirSync(assets, { recursive: true });
  const json = JSON.stringify({ islands: scope.ISLANDS, names: scope.NAMES });
  fs.writeFileSync(path.join(assets, "names.json"), json, "utf8");

  const kb = (Buffer.byteLength(json, "utf8") / 1024).toFixed(0);
  console.log("built android/.../assets/names.json  (" + kb + " KB, 99 names, 11 islands)");
}

/* Launcher icons for the native app, drawn from the same crescent as the web
   favicon so the two builds look like one app. */
function writeAndroidIcons() {
  const res = path.join(__dirname, "android", "app", "src", "main", "res");

  // legacy icons: the full tile, plus a circular cut for round launchers
  const LEGACY = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
  // adaptive foreground is 108dp with only the middle 72dp guaranteed visible
  const FOREGROUND = { mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432 };
  const SAFE_ZONE = 0.62;

  let written = 0;
  for (const [density, size] of Object.entries(LEGACY)) {
    const dir = path.join(res, "mipmap-" + density);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(path.join(dir, "ic_launcher.png"), drawIcon(size));
    fs.writeFileSync(path.join(dir, "ic_launcher_round.png"), drawIcon(size, { circle: true }));
    written += 2;
  }
  for (const [density, size] of Object.entries(FOREGROUND)) {
    const dir = path.join(res, "mipmap-" + density);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "ic_launcher_foreground.png"),
      drawIcon(size, { background: false, scale: SAFE_ZONE })
    );
    written += 1;
  }

  const adaptive = [
    '<?xml version="1.0" encoding="utf-8"?>',
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">',
    '    <background android:drawable="@color/ic_launcher_background" />',
    '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />',
    '    <monochrome android:drawable="@mipmap/ic_launcher_foreground" />',
    '</adaptive-icon>',
    '',
  ].join("\n");

  const anydpi = path.join(res, "mipmap-anydpi-v26");
  fs.mkdirSync(anydpi, { recursive: true });
  fs.writeFileSync(path.join(anydpi, "ic_launcher.xml"), adaptive, "utf8");
  fs.writeFileSync(path.join(anydpi, "ic_launcher_round.xml"), adaptive, "utf8");

  const values = path.join(res, "values");
  fs.mkdirSync(values, { recursive: true });
  fs.writeFileSync(
    path.join(values, "ic_launcher_background.xml"),
    '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n' +
    '    <color name="ic_launcher_background">#141A3A</color>\n</resources>\n',
    "utf8"
  );

  console.log("built android launcher icons     (" + written + " PNGs + adaptive icon)");
}

build();
