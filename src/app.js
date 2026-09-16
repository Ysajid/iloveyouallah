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

    var deep = svgEl("linearGradient", { id: "deep", x1: "0", y1: "0", x2: "0.3", y2: "1" });
    [["0%", "#16204d"], ["35%", "#0e1738"], ["70%", "#0a1130"], ["100%", "#070c22"]]
      .forEach(function (stop) {
        deep.appendChild(svgEl("stop", { offset: stop[0], "stop-color": stop[1] }));
      });
    defs.appendChild(deep);

    // the moon, off the top-left, and the light it lays on the water
    var moon = svgEl("radialGradient", { id: "moonglow" });
    moon.appendChild(svgEl("stop", { offset: "0%", "stop-color": "#f4c95d", "stop-opacity": ".30" }));
    moon.appendChild(svgEl("stop", { offset: "60%", "stop-color": "#f4c95d", "stop-opacity": ".07" }));
    moon.appendChild(svgEl("stop", { offset: "100%", "stop-color": "#f4c95d", "stop-opacity": "0" }));
    defs.appendChild(moon);

    ISLANDS.forEach(function (isl) {
      // lit from the top-left, like the moon is
      var g = svgEl("linearGradient", { id: "land" + isl.i, x1: "0.2", y1: "0", x2: "0.8", y2: "1" });
      g.appendChild(svgEl("stop", { offset: "0%", "stop-color": "hsl(" + isl.hue + " 46% 46%)" }));
      g.appendChild(svgEl("stop", { offset: "55%", "stop-color": "hsl(" + isl.hue + " 42% 33%)" }));
      g.appendChild(svgEl("stop", { offset: "100%", "stop-color": "hsl(" + isl.hue + " 40% 24%)" }));
      defs.appendChild(g);

      var shallow = svgEl("radialGradient", { id: "shallow" + isl.i });
      shallow.appendChild(svgEl("stop", { offset: "40%", "stop-color": "hsl(" + isl.hue + " 45% 45%)", "stop-opacity": ".00" }));
      shallow.appendChild(svgEl("stop", { offset: "62%", "stop-color": "hsl(185 60% 55%)", "stop-opacity": ".22" }));
      shallow.appendChild(svgEl("stop", { offset: "100%", "stop-color": "hsl(190 60% 50%)", "stop-opacity": "0" }));
      defs.appendChild(shallow);
    });

    var fog = svgEl("radialGradient", { id: "fog" });
    fog.appendChild(svgEl("stop", { offset: "35%", "stop-color": "#8f9ac4", "stop-opacity": ".20" }));
    fog.appendChild(svgEl("stop", { offset: "100%", "stop-color": "#8f9ac4", "stop-opacity": "0" }));
    defs.appendChild(fog);

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
        stroke: "#7fb4d8",
        "stroke-width": y % 11 < 5.5 ? 0.22 : 0.13,
        "stroke-opacity": 0.11,
        "stroke-linecap": "round",
      }));
    }
    return g;
  }

  /* Each island is its own little picture, so it keeps its shape whatever
     the chart is stretched to. The ocean behind it may stretch freely. */
  function islandArt(isl, open) {
    var svg = svgEl("svg", { viewBox: "-16 -10 132 138", "aria-hidden": "true" });
    var coast = COASTS[isl.seed % COASTS.length];

    svg.appendChild(svgEl("ellipse", {
      cx: 50, cy: 52,
      rx: open ? 64 : 50, ry: open ? 44 : 34,
      fill: open ? "url(#shallow" + isl.i + ")" : "url(#fog)",
    }));

    // the cliff side: a darker copy pushed down. This is what reads as depth.
    svg.appendChild(svgEl("path", {
      d: coast,
      transform: "translate(0,10)",
      fill: open ? "hsl(" + isl.hue + " 38% 14%)" : "#252c4b",
      opacity: open ? 1 : 0.5,
    }));
    // a pale beach where land meets water
    svg.appendChild(svgEl("path", {
      d: coast,
      transform: "translate(0,3.5)",
      fill: "none",
      stroke: "#e7dfc6",
      "stroke-width": 3,
      "stroke-opacity": open ? 0.22 : 0.06,
    }));
    // the lit top face
    svg.appendChild(svgEl("path", {
      d: coast,
      fill: open ? "url(#land" + isl.i + ")" : "#363d66",
      opacity: open ? 1 : 0.62,
    }));
    svg.appendChild(svgEl("path", {
      d: coast,
      fill: "none",
      stroke: open ? "hsl(" + isl.hue + " 58% 64%)" : "#5a628c",
      "stroke-width": 2,
      "stroke-opacity": open ? 0.45 : 0.25,
    }));

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
    sea.appendChild(svgEl("rect", { x: 0, y: 0, width: MAP_W, height: MAP_H, fill: "url(#deep)" }));
    sea.appendChild(svgEl("ellipse", { cx: 18, cy: 12, rx: 72, ry: 58, fill: "url(#moonglow)" }));
    sea.appendChild(swell());

    for (var k = 0; k < ISLANDS.length - 1; k++) {
      var from = ISLANDS[k], to = ISLANDS[k + 1];
      var sailed = (S.prog.islands[from.i] || {}).done;
      sea.appendChild(svgEl("path", {
        d: legPath(from, to, k % 2 ? 7 : -7),
        fill: "none",
        stroke: sailed ? "#f4c95d" : "#9fb0d8",
        "stroke-width": sailed ? 0.7 : 0.5,
        "stroke-opacity": sailed ? 0.75 : 0.26,
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
      btn.style.width = (ISLE_W * 1.32 * isl.size) + "%";
      btn.style.setProperty("--h", isl.hue);
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

  /* ---------- sky ---------- */

  function sprinkleStars() {
    var sky = el("stars");
    var frag = document.createDocumentFragment();
    for (var k = 0; k < 70; k++) {
      var s = make("i");
      s.style.left = (Math.random() * 100) + "%";
      s.style.top = (Math.random() * 100) + "%";
      s.style.animationDelay = (Math.random() * 4) + "s";
      var size = Math.random() < 0.15 ? 3 : 2;
      s.style.width = size + "px";
      s.style.height = size + "px";
      frag.appendChild(s);
    }
    sky.appendChild(frag);
  }

  /* ---------- wiring ---------- */

  function boot() {
    sprinkleStars();
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
