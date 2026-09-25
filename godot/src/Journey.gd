extends Node
## The 99 names and the eleven islands, read once at startup.
##
## The file is written by `node build.js` at the repo root, from src/names.js —
## the same source the web build and the Compose build read. Nothing here is
## hand-edited.

const DATA_PATH := "res://data/names.json"
const NAMES_PER_ISLAND := 9

enum Lang { BN, EN }

var islands: Array[Dictionary] = []
var names: Array[Dictionary] = []
var coasts: PackedStringArray = []

var _by_island: Dictionary = {}


func _ready() -> void:
	load_data()


func load_data() -> void:
	var file := FileAccess.open(DATA_PATH, FileAccess.READ)
	assert(file != null, "names.json missing — run `node build.js` at the repo root")
	var parsed: Variant = JSON.parse_string(file.get_as_text())
	file.close()
	assert(parsed is Dictionary, "names.json is not an object")

	for entry: Variant in parsed.get("islands", []):
		islands.append(entry)
	for entry: Variant in parsed.get("names", []):
		names.append(entry)
	for coast: Variant in parsed.get("coasts", []):
		coasts.append(coast)

	for entry: Dictionary in names:
		var key: int = int(entry["i"])
		if not _by_island.has(key):
			_by_island[key] = []
		_by_island[key].append(entry)


func island_count() -> int:
	return islands.size()


func island(number: int) -> Dictionary:
	for entry: Dictionary in islands:
		if int(entry["i"]) == number:
			return entry
	return {}


func names_of(island_number: int) -> Array:
	return _by_island.get(island_number, [])


func in_traditional_order() -> Array:
	var sorted := names.duplicate()
	sorted.sort_custom(func(a: Dictionary, b: Dictionary) -> bool: return int(a["n"]) < int(b["n"]))
	return sorted


## The three lines that get read aloud, in whichever language is on.
static func lines(entry: Dictionary, lang: Lang) -> Dictionary:
	return entry["bn"] if lang == Lang.BN else entry["en"]


static func title_of(entry: Dictionary, lang: Lang) -> String:
	return entry["bn"] if lang == Lang.BN else entry["en"]


const BN_DIGITS := ["০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯"]

## Numerals a Bangla-reading child can actually read.
static func number_in(value: int, lang: Lang) -> String:
	var plain := str(value)
	if lang != Lang.BN:
		return plain
	var out := ""
	for ch: String in plain:
		out += BN_DIGITS[ch.to_int()] if ch.is_valid_int() else ch
	return out
