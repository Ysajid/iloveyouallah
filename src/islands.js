/* The eleven islands, in journey order. One island a night.
   `bn` is what the kids see by default; `en` is the toggle.

   x, y place the island on the chart: x across (0 = west edge, 1 = east),
   y down the route (0 = where you start, 1 = the last island). The chart is
   taller than the screen, so the journey is something you scroll along.
   Both the web build and the Android build read these, so the map looks the
   same in each.

   size nudges a few islands bigger or smaller so the archipelago does not
   look stamped out, and seed picks which coastline shape it gets. */
const ISLANDS = [
  { i: 1,  emoji: "🤍", bn: "মায়ার দ্বীপ",      en: "Island of Mercy",       hue: 340, x: 0.30, y: 0.035, size: 1.00, seed: 0 },
  { i: 2,  emoji: "🕊️", bn: "ক্ষমার দ্বীপ",      en: "Island of Forgiveness", hue: 200, x: 0.64, y: 0.122, size: 0.92, seed: 1 },
  { i: 3,  emoji: "👑", bn: "রাজার দ্বীপ",      en: "Island of the King",    hue: 45,  x: 0.33, y: 0.210, size: 1.08, seed: 2 },
  { i: 4,  emoji: "🌱", bn: "সৃষ্টির দ্বীপ",     en: "Island of Making",      hue: 130, x: 0.69, y: 0.298, size: 0.95, seed: 3 },
  { i: 5,  emoji: "🎁", bn: "দানের দ্বীপ",      en: "Island of Giving",      hue: 20,  x: 0.36, y: 0.386, size: 1.02, seed: 4 },
  { i: 6,  emoji: "💡", bn: "আলোর দ্বীপ",       en: "Island of Light",       hue: 55,  x: 0.68, y: 0.474, size: 0.90, seed: 0 },
  { i: 7,  emoji: "🔭", bn: "জ্ঞানের দ্বীপ",     en: "Island of Knowing",     hue: 260, x: 0.31, y: 0.562, size: 1.05, seed: 1 },
  { i: 8,  emoji: "⚡", bn: "শক্তির দ্বীপ",      en: "Island of Strength",    hue: 15,  x: 0.66, y: 0.650, size: 0.98, seed: 2 },
  { i: 9,  emoji: "⚖️", bn: "ন্যায়ের দ্বীপ",     en: "Island of Fairness",    hue: 165, x: 0.34, y: 0.738, size: 0.93, seed: 3 },
  { i: 10, emoji: "♾️", bn: "চিরকালের দ্বীপ",   en: "Island of Forever",     hue: 230, x: 0.68, y: 0.826, size: 1.00, seed: 4 },
  { i: 11, emoji: "🏔️", bn: "মহিমার দ্বীপ",     en: "Island of Greatness",   hue: 285, x: 0.47, y: 0.930, size: 1.22, seed: 2 },
];

/* Five coastlines, as closed paths on a 100x100 box centred on 50,50.
   Hand-rolled rather than random so every build draws the same archipelago
   and the kids learn the shape of their own map. */
const COASTS = [
  "M12,56 C10,40 24,28 40,26 C54,24 64,16 76,22 C90,29 92,44 88,56 C84,70 70,78 54,79 C36,80 16,72 12,56 Z",
  "M14,52 C16,36 28,22 46,22 C62,22 74,28 84,40 C92,50 90,64 78,72 C64,81 44,82 30,76 C18,71 12,62 14,52 Z",
  "M16,58 C12,44 20,30 34,24 C50,17 68,18 80,28 C92,38 94,54 86,64 C76,76 58,82 42,80 C28,78 19,70 16,58 Z",
  "M13,50 C18,34 32,24 50,25 C66,26 80,32 87,44 C93,55 88,68 74,74 C58,81 38,80 25,72 C15,66 11,58 13,50 Z",
  "M15,54 C14,38 26,25 44,23 C60,21 76,24 85,35 C94,46 91,62 80,70 C67,79 47,81 32,75 C20,70 15,63 15,54 Z",
];
