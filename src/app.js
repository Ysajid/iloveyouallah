/* ---------------------------------------------------------------
   The 99 Names Journey — app logic

   Everything lives in localStorage so the file works on its own,
   offline, straight from the downloaded HTML. No network, ever.
   --------------------------------------------------------------- */

(function () {
  "use strict";

  /* ---------- storage (never let a blocked localStorage break the app) ---------- */

  var MEM = {};                       // fallback when storage is unavailable
  var K = "nj99.";

  function get(key) {
    try {
      var v = window.localStorage.getItem(K + key);
      return v === null ? undefined : v;
    } catch (e) {
      return MEM[key];
    }
  }

  function set(key, value) {
    MEM[key] = value;
    try { window.localStorage.setItem(K + key, value); } catch (e) { /* private mode */ }
  }

  function del(key) {
    delete MEM[key];
    try { window.localStorage.removeItem(K + key); } catch (e) { /* ignore */ }
  }

  function getJSON(key, fallback) {
    var raw = get(key);
    if (raw === undefined) return fallback;
    try { return JSON.parse(raw); } catch (e) { return fallback; }
  }

  function setJSON(key, value) { set(key, JSON.stringify(value)); }

  /* ---------- words that are not name content ---------- */

  var UI = {
    bn: {
      pickWho:    "কে খেলবে?",
      pickSub:    "নাম বেছে নাও, বা নতুন নাম লিখো।",
      newName:    "নতুন নাম",
      add:        "যোগ করো",
      journey:    "৯৯ নামের যাত্রা",
      subtitle:   "এগারো দ্বীপ · প্রতি রাতে একটা",
      islandsDone: function (a, b) { return a + " / " + b + " দ্বীপ শেষ"; },
      playingAs:  function (n) { return n + " খেলছে"; },
      nth:        function (k) { return k + " নম্বর দ্বীপ"; },
      locked:     "আগের দ্বীপটা আগে শেষ করো",
      start:      "শুরু করো",
      again:      "আবার দেখো",
      next:       "পরেরটা",
      back:       "আগেরটা",
      toGame:     "এখন খেলা",
      listen:     "শোনো",
      todayLbl:   "আজকে এটা করো",
      gameTitle:  "মিলাও",
      gameSub:    "নামের সাথে মানে মিলাও।",
      round:      function (a, b) { return "রাউন্ড " + a + " / " + b; },
      wellDone:   "দ্বীপটা জ্বলে উঠল!",
      finishSub:  "আজকের কাজটা মনে রেখো। কালকে আবার নতুন দ্বীপ।",
      lastIsland: "এগারোটা দ্বীপ শেষ! সবগুলো নাম এখন তোমার।",
      backToMap:  "মানচিত্রে ফিরে যাও",
      allNames:   "৯৯টি নাম",
      allSub:     "চিরাচরিত ক্রমে সাজানো",
      viewAll:    "৯৯টি নাম দেখো",
      switchWho:  "কে খেলবে বদলাও",
      close:      "বন্ধ করো",
      removeQ:    function (n) { return n + "-এর সব অগ্রগতি মুছে ফেলবে?"; },
      noStore:    "এই ব্রাউজারে অগ্রগতি সেভ হচ্ছে না। ব্রাউজারের প্রাইভেট মোড বন্ধ করে দেখো।"
    },
    en: {
      pickWho:    "Who is playing?",
      pickSub:    "Pick a name, or add a new one.",
      newName:    "New name",
      add:        "Add",
      journey:    "The 99 Names Journey",
      subtitle:   "Eleven islands · one a night",
      islandsDone: function (a, b) { return a + " of " + b + " islands lit"; },
      playingAs:  function (n) { return "Playing as " + n; },
      nth:        function (k) { return "Island " + k; },
      locked:     "Finish the island before this one",
      start:      "Start",
      again:      "Read again",
      next:       "Next",
      back:       "Back",
      toGame:     "Play the game",
      listen:     "Listen",
      todayLbl:   "Try this today",
      gameTitle:  "Match them up",
      gameSub:    "Match each name to what it means.",
      round:      function (a, b) { return "Round " + a + " of " + b; },
      wellDone:   "The island is lit!",
      finishSub:  "Remember today's one thing. A new island tomorrow.",
      lastIsland: "All eleven islands done! Every name is yours now.",
      backToMap:  "Back to the map",
      allNames:   "The 99 Names",
      allSub:     "In the traditional order",
      viewAll:    "See all 99 names",
      switchWho:  "Change who is playing",
      close:      "Close",
      removeQ:    function (n) { return "Delete all of " + n + "'s progress?"; },
      noStore:    "This browser is not saving progress. Try turning off private mode."
    }
  };

  var BN_DIGITS = ["০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯"];

  function num(n) {
    if (S.lang !== "bn") return String(n);
    return String(n).replace(/\d/g, function (d) { return BN_DIGITS[+d]; });
  }

  function t() {
    var dict = UI[S.lang];
    var val = dict[arguments[0]];
    if (typeof val === "function") {
      return val.apply(null, Array.prototype.slice.call(arguments, 1));
    }
    return val;
  }

  /* ---------- state ---------- */

  var S = {
    lang: get("lang") === "en" ? "en" : "bn",
    who: null,
    prog: null,
    island: null,      // island number being read
    cardAt: 0,         // index 0..8 within the island
    game: null
  };

  var TOTAL_ISLANDS = ISLANDS.length;
  var PAIRS_PER_ROUND = 3;

  function namesOf(islandNo) {
    return NAMES.filter(function (x) { return x.i === islandNo; });
  }

  function islandMeta(no) {
    for (var k = 0; k < ISLANDS.length; k++) if (ISLANDS[k].i === no) return ISLANDS[k];
    return ISLANDS[0];
  }

  function blankProgress() { return { islands: {} }; }

  function islandProg(no) {
    if (!S.prog.islands[no]) S.prog.islands[no] = { seen: [], stars: 0, done: false };
    return S.prog.islands[no];
  }

  function saveProg() { if (S.who) setJSON("p." + S.who, S.prog); }

  function unlocked(no) {
    if (no === 1) return true;
    var before = S.prog.islands[no - 1];
    return !!(before && before.done);
  }

  function islandsLit() {
    var n = 0;
    for (var k = 1; k <= TOTAL_ISLANDS; k++) {
      if (S.prog.islands[k] && S.prog.islands[k].done) n++;
    }
    return n;
  }

  /* ---------- tiny DOM helpers ---------- */

  function el(id) { return document.getElementById(id); }

  function make(tag, cls, text) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined && text !== null) n.textContent = text;
    return n;
  }

  function clear(node) { while (node.firstChild) node.removeChild(node.firstChild); }

  function show(screen) {
    var all = document.querySelectorAll(".screen");
    for (var k = 0; k < all.length; k++) all[k].classList.remove("on");
    el(screen).classList.add("on");
    window.scrollTo(0, 0);
  }

  function shuffle(list) {
    var a = list.slice();
    for (var k = a.length - 1; k > 0; k--) {
      var j = Math.floor(Math.random() * (k + 1));
      var tmp = a[k]; a[k] = a[j]; a[j] = tmp;
    }
    return a;
  }

  function stars(n, max) {
    var s = "";
    for (var k = 0; k < (max || 3); k++) s += k < n ? "★" : "☆";
    return s;
  }

  /* ---------- language toggle ---------- */

  function setLang(next) {
    S.lang = next;
    set("lang", next);
    document.documentElement.lang = next === "bn" ? "bn" : "en";
    paintLangButtons();
    repaint();
  }

  function paintLangButtons() {
    var btns = document.querySelectorAll("[data-lang]");
    for (var k = 0; k < btns.length; k++) {
      btns[k].classList.toggle("on", btns[k].getAttribute("data-lang") === S.lang);
    }
  }

  function repaint() {
    var live = document.querySelector(".screen.on");
    var id = live ? live.id : "profiles";
    if (id === "profiles") paintProfiles();
    else if (id === "map") paintMap();
    else if (id === "card") paintCard();
    else if (id === "game") paintGame();
    else if (id === "finish") paintFinish();
    else if (id === "all") paintAll();
  }

  /* ---------- profiles ---------- */

  function profiles() { return getJSON("profiles", []); }

  function paintProfiles() {
    el("pw-title").textContent = t("pickWho");
    el("pw-sub").textContent = t("pickSub");
    el("pw-input").placeholder = t("newName");
    el("pw-add").textContent = t("add");

    var list = profiles();
    var box = el("pw-list");
    clear(box);

    list.forEach(function (name) {
      var row = make("div", "row");

      var prog = getJSON("p." + name, blankProgress());
      var lit = 0;
      for (var k = 1; k <= TOTAL_ISLANDS; k++) {
        if (prog.islands && prog.islands[k] && prog.islands[k].done) lit++;
      }

      var pick = make("button", "name");
      pick.appendChild(make("span", null, name));
      pick.appendChild(make("small", null, t("islandsDone", num(lit), num(TOTAL_ISLANDS))));
      pick.addEventListener("click", function () { useProfile(name); });

      var kill = make("button", "del", "✕");
      kill.setAttribute("aria-label", "delete");
      kill.addEventListener("click", function (ev) {
        ev.stopPropagation();
        if (!window.confirm(t("removeQ", name))) return;
        setJSON("profiles", profiles().filter(function (x) { return x !== name; }));
        del("p." + name);
        if (get("last") === name) del("last");
        paintProfiles();
      });

      row.appendChild(pick);
      row.appendChild(kill);
      box.appendChild(row);
    });

    el("pw-nostore").hidden = storageWorks();
    el("pw-nostore").textContent = t("noStore");
  }

  function storageWorks() {
    try {
      window.localStorage.setItem(K + "probe", "1");
      window.localStorage.removeItem(K + "probe");
      return true;
    } catch (e) { return false; }
  }

  function addProfile() {
    var input = el("pw-input");
    var name = (input.value || "").trim().slice(0, 24);
    if (!name) return;
    var list = profiles();
    if (list.indexOf(name) === -1) {
      list.push(name);
      setJSON("profiles", list);
    }
    input.value = "";
    useProfile(name);
  }

  function useProfile(name) {
    S.who = name;
    set("last", name);
    S.prog = getJSON("p." + name, blankProgress());
    if (!S.prog.islands) S.prog.islands = {};
    paintMap();
    show("map");
  }

  /* ---------- the map ---------- */

  /* ---------- the map: an ocean chart with the islands on it ---------- */

  var MAP_W = 100;          // chart width in svg units
  var MAP_H = 300;          // chart height — taller than the screen, so you scroll it
  var ISLE_W = 26;          // an island's width at size 1
  var PLATE_TUCK = 0.18;    // how far the label sits up over the shore
  var GAP_PAD = 12;         // clear water between a label and the next island
  var ART_MAX = 190;        // an island stops growing past this, or a tablet
                            // ends up scrolling five screens to reach the end

  /* An island's drawn size. Matches Chart.artSizeDp in the native build. */
  function artSize(widthPx, isl) {
    return Math.min(widthPx * (ISLE_W / 100) * 1.32 * isl.size, ART_MAX * isl.size);
  }

  /* How tall the chart has to be so no label reaches the island below it.
     Island spacing is a fraction of the chart height while the labels are a
     fixed size in points, so a short chart on a small screen is exactly where
     labels collide. Checked pair by pair: the closest gap and the biggest
     island are not the same pair, and treating them as one made the chart far
     longer to scroll than it needed to be.

     The same rule runs in the native build — see Chart.chartHeightDp. */
  function chartHeight(widthPx, plateH) {
    var forLooks = widthPx * (widthPx >= 700 ? 2.6 : widthPx >= 560 ? 3.1 : 4.4);
    var toFit = 0;
    for (var k = 0; k < ISLANDS.length - 1; k++) {
      var gap = ISLANDS[k + 1].y - ISLANDS[k].y;
      if (gap <= 0) continue;
      var box = artSize(widthPx, ISLANDS[k]) * (1 - PLATE_TUCK) + plateH;
      toFit = Math.max(toFit, (box + GAP_PAD) / gap);
    }
    return Math.max(forLooks, toFit);
  }

  /* Set the height from the labels actually on screen, then once more from
     their measured height. Islands are positioned as a percentage of the
     chart, so they follow the new height without being rebuilt. */
  function sizeChart(chart) {
    var w = chart.clientWidth;
    if (!w) return;

    var isles = chart.querySelectorAll(".isle");
    for (var i = 0; i < isles.length; i++) {
      var isl = ISLANDS[i];
      if (isl) isles[i].style.width = artSize(w, isl) + "px";
    }

    chart.style.height = Math.round(chartHeight(w, 58)) + "px";
    var plates = chart.querySelectorAll(".isle .plate");
    var tallest = 0;
    for (var k = 0; k < plates.length; k++) {
      tallest = Math.max(tallest, plates[k].offsetHeight);
    }
    if (tallest > 0) {
      chart.style.height = Math.round(chartHeight(w, tallest)) + "px";
    }
  }

  /* One island colour, lightened or darkened. Keeps every face of the land
     on the same hue so it reads as one object lit from one side. */
  function shade(isl, dl, ds) {
    var sat = Math.max(0, Math.min(100, isl.sat + (ds || 0)));
    var light = Math.max(0, Math.min(100, isl.light + dl));
    return "hsl(" + isl.hue + " " + sat + "% " + light + "%)";
  }

  function mapX(isl) { return isl.x * MAP_W; }
  function mapY(isl) { return isl.y * MAP_H; }

  function svgEl(tag, attrs) {
    var n = document.createElementNS("http://www.w3.org/2000/svg", tag);
    for (var k in attrs) if (attrs.hasOwnProperty(k)) n.setAttribute(k, attrs[k]);
    return n;
  }

  /* One leg of the sailing route, bent so the line looks sailed rather than ruled. */
  function legPath(from, to, pull) {
    var x1 = mapX(from), y1 = mapY(from), x2 = mapX(to), y2 = mapY(to);
    var mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
    // push the midpoint sideways, alternating, for a lazy S down the chart
    var nx = -(y2 - y1), ny = x2 - x1;
    var len = Math.sqrt(nx * nx + ny * ny) || 1;
    return "M" + x1 + "," + y1 +
           " Q" + (mx + nx / len * pull) + "," + (my + ny / len * pull) +
           " " + x2 + "," + y2;
  }

  function oceanDefs() {
    var defs = svgEl("defs");

    // shallow tropical water: pale at the top where the sun is, deeper below
    var water = svgEl("linearGradient", { id: "water", x1: "0", y1: "0", x2: "0.25", y2: "1" });
    [["0%", "#cdeaf1"], ["30%", "#a9dce8"], ["68%", "#7ec9da"], ["100%", "#5fb7cc"]]
      .forEach(function (stop) {
        water.appendChild(svgEl("stop", { offset: stop[0], "stop-color": stop[1] }));
      });
    defs.appendChild(water);

    // sunlight off the top corner
    var sun = svgEl("radialGradient", { id: "sunglow" });
    sun.appendChild(svgEl("stop", { offset: "0%", "stop-color": "#fffdf0", "stop-opacity": ".72" }));
    sun.appendChild(svgEl("stop", { offset: "55%", "stop-color": "#fff6d8", "stop-opacity": ".22" }));
    sun.appendChild(svgEl("stop", { offset: "100%", "stop-color": "#fff6d8", "stop-opacity": "0" }));
    defs.appendChild(sun);

    ISLANDS.forEach(function (isl) {
      // lit from the top-left, like the sun is
      var g = svgEl("linearGradient", { id: "land" + isl.i, x1: "0.15", y1: "0", x2: "0.75", y2: "1" });
      g.appendChild(svgEl("stop", { offset: "0%", "stop-color": shade(isl, 8) }));
      g.appendChild(svgEl("stop", { offset: "55%", "stop-color": shade(isl, 0) }));
      g.appendChild(svgEl("stop", { offset: "100%", "stop-color": shade(isl, -9) }));
      defs.appendChild(g);

      // the paler ring of water an island sits in
      var shallow = svgEl("radialGradient", { id: "shallow" + isl.i });
      shallow.appendChild(svgEl("stop", { offset: "38%", "stop-color": "#e6f7f3", "stop-opacity": "0" }));
      shallow.appendChild(svgEl("stop", { offset: "60%", "stop-color": "#dff5f0", "stop-opacity": ".70" }));
      shallow.appendChild(svgEl("stop", { offset: "100%", "stop-color": "#cfeef0", "stop-opacity": "0" }));
      defs.appendChild(shallow);
    });

    // islands not reached yet sit under a haze
    var soften = svgEl("filter", { id: "soften", x: "-30%", y: "-30%", width: "160%", height: "160%" });
    soften.appendChild(svgEl("feGaussianBlur", { stdDeviation: "2.4" }));
    defs.appendChild(soften);

    var haze = svgEl("radialGradient", { id: "haze" });
    haze.appendChild(svgEl("stop", { offset: "35%", "stop-color": "#ffffff", "stop-opacity": ".62" }));
    haze.appendChild(svgEl("stop", { offset: "100%", "stop-color": "#ffffff", "stop-opacity": "0" }));
    defs.appendChild(haze);

    return defs;
  }

  /* Swell lines across the whole chart. Cheap, and they read as water. */
  function swell() {
    var g = svgEl("g", { class: "swell" });
    for (var y = 4; y < MAP_H; y += 5.5) {
      var d = "M-12," + y.toFixed(1);
      for (var x = -12; x <= MAP_W + 12; x += 8) {
        var lift = (((x + y) % 3) - 1) * 0.5;
        d += " q4," + lift.toFixed(2) + " 8,0";
      }
      g.appendChild(svgEl("path", {
        d: d,
        fill: "none",
        stroke: "#ffffff",
        "stroke-width": y % 11 < 5.5 ? 0.26 : 0.15,
        "stroke-opacity": 0.30,
        "stroke-linecap": "round",
      }));
    }
    return g;
  }

  /* ---------------------------------------------------------------------
     Isometric islands.

     No 3D engine here: the coastline is a shape lying on the ground, and an
     oblique projection tilts that ground away from the viewer. Squash y,
     subtract height for z, and an outline becomes a solid you can walk around.

        screen.x = x
        screen.y = y * SQUASH - z

     The land is then extruded: every coastline edge on the near side becomes
     a wall, shaded by which way it faces. That per-face shading is what makes
     it read as a solid object rather than a sticker.
     --------------------------------------------------------------------- */

  var SQUASH = 0.54;        // how far the ground tilts away
  var ISLE_H = 0.46;        // island height, as a fraction of its radius
  var SAMPLES = 44;         // points around the coastline
  var LIGHT = { x: -0.55, y: -0.83 };   // sun, upper-left

  /* Each island draws in its own <svg>, so it carries its own gradient. */
  function islandDefs(isl) {
    var defs = svgEl("defs");
    var g = svgEl("linearGradient", { id: "top" + isl.i, x1: "0.15", y1: "0", x2: "0.75", y2: "1" });
    g.appendChild(svgEl("stop", { offset: "0%", "stop-color": shade(isl, 10) }));
    g.appendChild(svgEl("stop", { offset: "60%", "stop-color": shade(isl, 0) }));
    g.appendChild(svgEl("stop", { offset: "100%", "stop-color": shade(isl, -7) }));
    defs.appendChild(g);

    var soften = svgEl("filter", { id: "soft" + isl.i, x: "-40%", y: "-40%", width: "180%", height: "180%" });
    soften.appendChild(svgEl("feGaussianBlur", { stdDeviation: "2.2" }));
    defs.appendChild(soften);
    return defs;
  }

  function iso(x, y, z) {
    return { x: x, y: y * SQUASH - z };
  }

  /* Brightness of a wall facing outward along (nx, ny), 0 dark .. 1 lit. */
  function faceLight(nx, ny) {
    var d = nx * LIGHT.x + ny * LIGHT.y;         // -1 facing away .. 1 facing sun
    return 0.5 + 0.5 * d;
  }

  function polygon(points, fill, extra) {
    var attrs = {
      points: points.map(function (p) { return p.x.toFixed(2) + "," + p.y.toFixed(2); }).join(" "),
      fill: fill,
    };
    for (var k in extra) if (extra.hasOwnProperty(k)) attrs[k] = extra[k];
    return svgEl("polygon", attrs);
  }

  function islandArt(isl, open) {
    // needs its own <svg> root: a bare <g> has no SVG context to draw in
    var svg = svgEl("svg", { viewBox: "0 0 100 100", "aria-hidden": "true" });
    svg.appendChild(islandDefs(isl));
    var g = svgEl("g", { class: "isleart" });
    svg.appendChild(g);
    var pts = coastPoints(isl.seed, SAMPLES);

    // the art box is 2.6 radii wide, leaving room for the shadow and the drop
    var R = 41;
    var H = R * ISLE_H;
    var cx = 50, cy = 44;

    var top = pts.map(function (p) { 
      var q = iso(p.x * R, p.y * R, H);
      return { x: cx + q.x, y: cy + q.y };
    });
    var base = pts.map(function (p) {
      var q = iso(p.x * R, p.y * R, 0);
      return { x: cx + q.x, y: cy + q.y };
    });

    // --- the shadow it casts on the water ---
    var shadow = pts.map(function (p) {
      var q = iso(p.x * R * 1.02, p.y * R * 1.02, 0);
      return { x: cx + q.x + H * 0.55, y: cy + q.y + H * 0.30 };
    });
    g.appendChild(polygon(shadow, open ? "rgba(23,86,104,.22)" : "rgba(23,86,104,.10)"));

    // --- the shallows it sits in ---
    var shallows = pts.map(function (p) {
      var q = iso(p.x * R * 1.30, p.y * R * 1.30, 0);
      return { x: cx + q.x, y: cy + q.y };
    });
    g.appendChild(polygon(shallows, open ? "rgba(223,245,240,.62)" : "rgba(255,255,255,.55)", {
      filter: "url(#soft" + isl.i + ")",
    }));

    // --- the walls, near side only, each shaded by the way it faces ---
    for (var k = 0; k < SAMPLES; k++) {
      var j = (k + 1) % SAMPLES;
      // outward normal of this edge, on the ground plane
      var nx = (pts[k].x + pts[j].x) / 2;
      var ny = (pts[k].y + pts[j].y) / 2;
      var len = Math.hypot(nx, ny) || 1;
      nx /= len; ny /= len;
      if (ny <= 0) continue;               // facing away: hidden behind the top

      var lit = faceLight(nx, ny);
      var wall = open
        ? shade(isl, -16 - 16 * (1 - lit), 4)
        : "hsl(205 12% " + (58 + 10 * lit).toFixed(0) + "%)";
      g.appendChild(polygon([top[k], top[j], base[j], base[k]], wall));
    }

    // --- a band of sand just above the waterline ---
    var sandTop = pts.map(function (p) {
      var q = iso(p.x * R, p.y * R, H * 0.26);
      return { x: cx + q.x, y: cy + q.y };
    });
    for (var m = 0; m < SAMPLES; m++) {
      var n = (m + 1) % SAMPLES;
      var sx = (pts[m].x + pts[n].x) / 2, sy = (pts[m].y + pts[n].y) / 2;
      var sl = Math.hypot(sx, sy) || 1;
      if (sy / sl <= 0) continue;
      g.appendChild(polygon([sandTop[m], sandTop[n], base[n], base[m]],
        open ? "#f3e2bd" : "#dfe6e9"));
    }

    // --- the top: the island's own colour, lit from the upper left ---
    g.appendChild(polygon(top, open ? "url(#top" + isl.i + ")" : "#cfd9de"));
    // a soft inner highlight so the top does not read as flat card
    var crown = pts.map(function (p) {
      var q = iso(p.x * R * 0.62, p.y * R * 0.62 - R * 0.10, H);
      return { x: cx + q.x, y: cy + q.y };
    });
    g.appendChild(polygon(crown, open ? shade(isl, 9) : "#dae2e6", { opacity: open ? 0.55 : 0.4 }));

    return svg;
  }

  function paintMap() {
    el("map-title").textContent = t("journey");
    el("map-sub").textContent = t("subtitle");
    el("map-who").textContent = t("playingAs", S.who);
    el("map-all").textContent = t("viewAll");

    var lit = islandsLit();
    el("map-count").textContent = t("islandsDone", num(lit), num(TOTAL_ISLANDS));
    el("map-bar").style.width = (lit / TOTAL_ISLANDS * 100) + "%";

    var chart = el("map-chart");
    clear(chart);

    /* ---- the water, the moonlight and the route ---- */
    var sea = svgEl("svg", {
      class: "sea",
      viewBox: "0 0 " + MAP_W + " " + MAP_H,
      preserveAspectRatio: "none",
      "aria-hidden": "true",
    });
    sea.appendChild(oceanDefs());
    sea.appendChild(svgEl("rect", { x: 0, y: 0, width: MAP_W, height: MAP_H, fill: "url(#water)" }));
    sea.appendChild(svgEl("ellipse", { cx: 20, cy: 10, rx: 74, ry: 56, fill: "url(#sunglow)" }));
    sea.appendChild(swell());

    for (var k = 0; k < ISLANDS.length - 1; k++) {
      var from = ISLANDS[k], to = ISLANDS[k + 1];
      var sailed = (S.prog.islands[from.i] || {}).done;
      sea.appendChild(svgEl("path", {
        d: legPath(from, to, k % 2 ? 7 : -7),
        fill: "none",
        stroke: sailed ? "#e08c2c" : "#ffffff",
        "stroke-width": sailed ? 0.75 : 0.55,
        "stroke-opacity": sailed ? 0.9 : 0.6,
        "stroke-linecap": "round",
        "stroke-dasharray": "2.4 3.2",
      }));
    }
    chart.appendChild(sea);

    /* ---- the islands, each one its own tap target ---- */
    ISLANDS.forEach(function (isl) {
      var open = unlocked(isl.i);
      var p = S.prog.islands[isl.i] || { seen: [], stars: 0, done: false };

      var btn = make("button", "isle" + (open ? "" : " locked") + (p.done ? " done" : ""));
      btn.style.left = isl.x * 100 + "%";
      btn.style.top = isl.y * 100 + "%";
      btn.style.width = artSize(chart.clientWidth || 360, isl) + "px";
      btn.style.setProperty("--bob", (-isl.seed * 1.3) + "s");

      var art = make("div", "art");
      art.appendChild(islandArt(isl, open));
      art.appendChild(make("span", "em", open ? isl.emoji : "🔒"));
      if (p.done) art.appendChild(make("span", "flag", "✓"));
      btn.appendChild(art);

      // hangs below the island; still part of the button, so still tappable
      var plate = make("div", "plate");
      plate.appendChild(make("div", "nth", t("nth", num(isl.i))));
      plate.appendChild(make("div", "ttl", S.lang === "bn" ? isl.bn : isl.en));
      if (open) {
        var st = make("div", "stars");
        for (var q = 0; q < 3; q++) st.appendChild(make("span", q < p.stars ? "" : "off", "★"));
        plate.appendChild(st);
      } else {
        plate.appendChild(make("div", "stars locked-note", t("locked")));
      }
      btn.appendChild(plate);

      if (open) {
        btn.addEventListener("click", function () { openIsland(isl.i); });
      } else {
        btn.disabled = true;
      }
      chart.appendChild(btn);
    });

    /* ---- the boat marks the island they are up to ---- */
    var next = null;
    for (var n = 0; n < ISLANDS.length; n++) {
      if (unlocked(ISLANDS[n].i) && !(S.prog.islands[ISLANDS[n].i] || {}).done) {
        next = ISLANDS[n];
        break;
      }
    }
    if (next) {
      var boat = make("div", "boat", "⛵");
      boat.style.left = (next.x * 100 + (next.x > 0.5 ? -19 : 19)) + "%";
      boat.style.top = (next.y * 100 - 1.2) + "%";
      chart.appendChild(boat);
    }

    sizeChart(chart);
  }

  /* ---------- reading the nine cards ---------- */

  function openIsland(no) {
    S.island = no;
    var p = islandProg(no);
    // if they have read everything already, start over at the first card
    S.cardAt = p.seen.length >= 9 ? 0 : Math.min(p.seen.length, 8);
    paintCard();
    show("card");
  }

  function paintCard() {
    var list = namesOf(S.island);
    var item = list[S.cardAt];
    var isl = islandMeta(S.island);
    var p = islandProg(S.island);
    var text = S.lang === "bn" ? item.bn : item.en;

    el("card-island").textContent = S.lang === "bn" ? isl.bn : isl.en;

    var pips = el("card-pips");
    clear(pips);
    for (var k = 0; k < list.length; k++) {
      var cls = k === S.cardAt ? "now" : (p.seen.indexOf(list[k].n) !== -1 ? "seen" : "");
      pips.appendChild(make("i", cls));
    }

    el("card-num").textContent = t("nth", num(S.island)) + " · " + num(S.cardAt + 1) + "/" + num(9);
    el("card-ar").textContent = item.ar;
    el("card-tr").textContent = item.tr;
    el("card-meaning").textContent = text.meaning;
    el("card-about").textContent = text.about;
    el("today-lbl").textContent = t("todayLbl");
    el("today-text").textContent = text.today;

    wireAudio(item.n);

    var back = el("card-back");
    var fwd = el("card-next");
    back.textContent = t("back");
    back.disabled = S.cardAt === 0;
    back.style.opacity = S.cardAt === 0 ? ".4" : "1";
    fwd.textContent = S.cardAt === 8 ? t("toGame") : t("next");

    // mark as read
    if (p.seen.indexOf(item.n) === -1) {
      p.seen.push(item.n);
      if (p.seen.length >= 9 && p.stars < 1) p.stars = 1;
      saveProg();
    }
  }

  /* ---------- audio: works the day the clips land in ./audio, silent until then ---------- */

  var audioCache = {};

  function clipPath(n) {
    return "audio/" + (n < 10 ? "0" + n : String(n)) + ".mp3";
  }

  function wireAudio(n) {
    var btn = el("card-say");
    btn.hidden = true;
    btn.classList.remove("playing");
    btn.textContent = "🔊 " + t("listen");

    var clip = audioCache[n];
    if (clip === "missing") return;

    if (!clip) {
      clip = new Audio(clipPath(n));
      clip.preload = "auto";
      audioCache[n] = clip;
      clip.addEventListener("error", function () {
        audioCache[n] = "missing";
        if (el("card-say")) el("card-say").hidden = true;
      });
      clip.addEventListener("canplaythrough", function () {
        // only reveal if this card is still the one on screen
        if (namesOf(S.island)[S.cardAt] && namesOf(S.island)[S.cardAt].n === n) {
          el("card-say").hidden = false;
        }
      });
    } else if (clip.readyState >= 3) {
      btn.hidden = false;
    }

    btn.onclick = function () {
      var c = audioCache[n];
      if (!c || c === "missing") return;
      c.currentTime = 0;
      btn.classList.add("playing");
      c.onended = function () { btn.classList.remove("playing"); };
      var maybe = c.play();
      if (maybe && maybe.catch) maybe.catch(function () { btn.classList.remove("playing"); });
    };
  }

  /* ---------- the matching game ---------- */

  function startGame() {
    var list = shuffle(namesOf(S.island));
    var rounds = [];
    for (var k = 0; k < list.length; k += PAIRS_PER_ROUND) {
      rounds.push(list.slice(k, k + PAIRS_PER_ROUND));
    }
    S.game = { rounds: rounds, at: 0, mistakes: 0, pickedName: null, pickedMeaning: null, matched: 0 };
    paintGame();
    show("game");
  }

  function paintGame() {
    var g = S.game;
    if (!g) { startGame(); return; }

    var round = g.rounds[g.at];
    el("game-title").textContent = t("gameTitle");
    el("game-sub").textContent = t("gameSub");
    el("game-round").textContent = t("round", num(g.at + 1), num(g.rounds.length));

    var left = el("game-names");
    var right = el("game-meanings");
    clear(left);
    clear(right);
    g.pickedName = null;
    g.pickedMeaning = null;
    g.matched = 0;

    round.forEach(function (item) {
      var tile = make("button", "tile arabic", item.ar);
      tile.dataset.n = item.n;
      tile.addEventListener("click", function () { pick(tile, "name"); });
      left.appendChild(tile);
    });

    shuffle(round).forEach(function (item) {
      var txt = (S.lang === "bn" ? item.bn : item.en).meaning;
      var tile = make("button", "tile", txt);
      tile.dataset.n = item.n;
      tile.addEventListener("click", function () { pick(tile, "meaning"); });
      right.appendChild(tile);
    });
  }

  function pick(tile, side) {
    var g = S.game;
    if (tile.classList.contains("right")) return;

    var key = side === "name" ? "pickedName" : "pickedMeaning";
    var other = side === "name" ? "pickedMeaning" : "pickedName";

    if (g[key] === tile) {                 // tapping the same tile clears it
      tile.classList.remove("picked");
      g[key] = null;
      return;
    }
    if (g[key]) g[key].classList.remove("picked");
    g[key] = tile;
    tile.classList.add("picked");

    if (!g[other]) return;

    var a = g[key], b = g[other];
    if (a.dataset.n === b.dataset.n) {
      a.classList.remove("picked"); b.classList.remove("picked");
      a.classList.add("right"); b.classList.add("right");
      g.pickedName = null; g.pickedMeaning = null;
      g.matched++;
      if (g.matched >= g.rounds[g.at].length) window.setTimeout(nextRound, 420);
    } else {
      g.mistakes++;
      a.classList.add("wrong"); b.classList.add("wrong");
      var pair = [a, b];
      window.setTimeout(function () {
        pair.forEach(function (x) { x.classList.remove("wrong", "picked"); });
      }, 340);
      g.pickedName = null; g.pickedMeaning = null;
    }
  }

  function nextRound() {
    var g = S.game;
    g.at++;
    if (g.at < g.rounds.length) { paintGame(); return; }

    var p = islandProg(S.island);
    var earned = g.mistakes === 0 ? 3 : 2;
    if (earned > p.stars) p.stars = earned;
    p.done = true;
    saveProg();
    paintFinish();
    show("finish");
  }

  /* ---------- island finished ---------- */

  function paintFinish() {
    var isl = islandMeta(S.island);
    var p = islandProg(S.island);
    var lastCard = namesOf(S.island)[8];
    var text = S.lang === "bn" ? lastCard.bn : lastCard.en;
    var allDone = islandsLit() >= TOTAL_ISLANDS;

    el("fin-em").textContent = isl.emoji;
    el("fin-title").textContent = t("wellDone");

    var got = el("fin-stars");
    clear(got);
    for (var k = 0; k < 3; k++) got.appendChild(make("span", k < p.stars ? "" : "off", "★"));

    el("fin-name").textContent = S.lang === "bn" ? isl.bn : isl.en;
    el("fin-sub").textContent = allDone ? t("lastIsland") : t("finishSub");

    el("fin-today-lbl").textContent = t("todayLbl");
    el("fin-today").textContent = text.today;
    el("fin-back").textContent = t("backToMap");
  }

  /* ---------- the full 99, traditional order ---------- */

  function paintAll() {
    el("all-title").textContent = t("allNames");
    el("all-sub").textContent = t("allSub");

    var box = el("all-list");
    clear(box);

    NAMES.slice().sort(function (a, b) { return a.n - b.n; }).forEach(function (item) {
      var text = S.lang === "bn" ? item.bn : item.en;
      var row = make("button", "one");
      row.appendChild(make("span", "idx", num(item.n)));
      row.appendChild(make("span", "ar", item.ar));
      var txt = make("span", "txt");
      txt.appendChild(make("b", null, text.meaning));
      txt.appendChild(make("small", null, item.tr));
      row.appendChild(txt);
      row.addEventListener("click", function () { openSheet(item); });
      box.appendChild(row);
    });
  }

  function openSheet(item) {
    var text = S.lang === "bn" ? item.bn : item.en;
    el("sh-num").textContent = t("nth", num(item.i)) + " · " + num(item.n) + "/" + num(99);
    el("sh-ar").textContent = item.ar;
    el("sh-tr").textContent = item.tr;
    el("sh-meaning").textContent = text.meaning;
    el("sh-about").textContent = text.about;
    el("sh-today-lbl").textContent = t("todayLbl");
    el("sh-today").textContent = text.today;
    el("sheet").classList.add("on");
  }

  /* ---------- the light behind everything ---------- */

  /* Soft shapes drifting slowly, in the colours of the rug. Replaces the
     star field, which needed a dark screen to read at all. */
  function sprinkleBokeh() {
    var sky = el("bokeh");
    if (!sky) return;
    var tints = ["#f7cdb0", "#cdbfe0", "#a9dcc4", "#f5ce63", "#9fd8e4", "#f0907a"];
    var frag = document.createDocumentFragment();
    for (var k = 0; k < 10; k++) {
      var b = make("i");
      var size = 70 + (k % 5) * 40;
      b.style.width = size + "px";
      b.style.height = size + "px";
      b.style.left = ((k * 37) % 100) + "%";
      b.style.top = ((k * 53) % 100) + "%";
      b.style.background = tints[k % tints.length];
      b.style.opacity = k % 3 === 0 ? ".13" : ".09";
      b.style.animationDelay = (-k * 1.7) + "s";
      b.style.animationDuration = (18 + (k % 4) * 5) + "s";
      frag.appendChild(b);
    }
    sky.appendChild(frag);
  }

  /* ---------- wiring ---------- */

  function boot() {
    sprinkleBokeh();
    document.documentElement.lang = S.lang === "bn" ? "bn" : "en";
    paintLangButtons();

    var langBtns = document.querySelectorAll("[data-lang]");
    for (var k = 0; k < langBtns.length; k++) {
      (function (b) {
        b.addEventListener("click", function () { setLang(b.getAttribute("data-lang")); });
      })(langBtns[k]);
    }

    el("pw-add").addEventListener("click", addProfile);
    el("pw-input").addEventListener("keydown", function (ev) {
      if (ev.key === "Enter") addProfile();
    });

    el("card-back").addEventListener("click", function () {
      if (S.cardAt > 0) { S.cardAt--; paintCard(); }
    });
    el("card-next").addEventListener("click", function () {
      if (S.cardAt < 8) { S.cardAt++; paintCard(); }
      else startGame();
    });
    el("card-quit").addEventListener("click", function () { paintMap(); show("map"); });

    el("game-quit").addEventListener("click", function () { paintMap(); show("map"); });
    el("fin-back").addEventListener("click", function () { paintMap(); show("map"); });

    el("map-all").addEventListener("click", function () { paintAll(); show("all"); });
    el("all-back").addEventListener("click", function () { paintMap(); show("map"); });
    el("map-who").addEventListener("click", function () { paintProfiles(); show("profiles"); });

    el("sheet").addEventListener("click", function (ev) {
      if (ev.target === el("sheet")) el("sheet").classList.remove("on");
    });
    el("sh-close").addEventListener("click", function () {
      el("sheet").classList.remove("on");
    });

    window.addEventListener("resize", function () {
      var chart = el("map-chart");
      if (chart && chart.childNodes.length) sizeChart(chart);
    });

    // straight back into the last journey, if there is one
    var last = get("last");
    if (last && profiles().indexOf(last) !== -1) {
      useProfile(last);
    } else {
      paintProfiles();
      show("profiles");
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
