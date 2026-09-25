class_name Palette
extends RefCounted
## The playroom colours, same values as src/styles.css and Theme.kt.

const PAPER := Color("fbf6ec")
const SAND := Color("f3e9d8")
const CARD := Color("fffdf8")
const LINE := Color("e7dcc7")

const INK := Color("3b332a")
const INK_SOFT := Color("6e6455")
const INK_FAINT := Color("736a5e")

const BUTTER := Color("f5ce63")
const APRICOT := Color("f0a24e")
const ON_GOLD := Color("4a3208")
const TEAL := Color("2e7d8a")
const SKY := Color("63c0d1")
const GREEN := Color("6bb886")
const ROSE := Color("dd6459")
const EMPTY_STAR := Color("dccfb6")

const BEACH := Color("f6e6c4")
const WATER_SHALLOW := Color("bfe8ef")
const WATER_DEEP := Color("3f9fb8")
const SUN := Color("fff6dc")

## One island's colour, lightened or darkened. Every face stays on one hue so
## the land reads as a single object lit from one side.
static func island_colour(isl: Dictionary, dl: float = 0.0, ds: float = 0.0) -> Color:
	var h: float = float(isl["hue"]) / 360.0
	var s: float = clampf((float(isl["sat"]) + ds) / 100.0, 0.0, 1.0)
	var l: float = clampf((float(isl["light"]) + dl) / 100.0, 0.0, 1.0)
	return Color.from_hsv(h, s * 0.85, l + (1.0 - l) * 0.15)
