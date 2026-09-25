extends Node
## Who is playing, how far each of them got, and which language they read in.
##
## Kept in user:// so it survives the app closing. Same rules as the other two
## builds: one star for reading all nine cards, two for finishing the matching
## game, three for finishing it without a wrong match, and stars never go down.

const SAVE_PATH := "user://journey.save"

var lang: Journey.Lang = Journey.Lang.BN
var last_profile: String = ""
var profiles: Array[String] = []

var _progress: Dictionary = {}   # name -> { island -> { seen, stars, done } }


func _ready() -> void:
	load_all()


func load_all() -> void:
	if not FileAccess.file_exists(SAVE_PATH):
		return
	var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
	if file == null:
		return
	var parsed: Variant = JSON.parse_string(file.get_as_text())
	file.close()
	if not (parsed is Dictionary):
		return   # a corrupt save costs stars, not bedtime

	lang = Journey.Lang.EN if parsed.get("lang", "bn") == "en" else Journey.Lang.BN
	last_profile = str(parsed.get("last", ""))
	profiles.clear()
	for name: Variant in parsed.get("profiles", []):
		profiles.append(str(name))
	_progress = parsed.get("progress", {})


func save_all() -> void:
	var file := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
	if file == null:
		return
	file.store_string(JSON.stringify({
		"lang": "en" if lang == Journey.Lang.EN else "bn",
		"last": last_profile,
		"profiles": profiles,
		"progress": _progress,
	}))
	file.close()


func add_profile(name: String) -> void:
	var clean := name.strip_edges().left(24)
	if clean.is_empty() or profiles.has(clean):
		return
	profiles.append(clean)
	save_all()


func remove_profile(name: String) -> void:
	profiles.erase(name)
	_progress.erase(name)
	if last_profile == name:
		last_profile = ""
	save_all()


func island_state(who: String, island: int) -> Dictionary:
	var mine: Dictionary = _progress.get(who, {})
	return mine.get(str(island), {"seen": [], "stars": 0, "done": false})


func unlocked(who: String, island: int) -> bool:
	return island == 1 or bool(island_state(who, island - 1).get("done", false))


func islands_lit(who: String) -> int:
	var mine: Dictionary = _progress.get(who, {})
	var count := 0
	for key: String in mine:
		if bool(mine[key].get("done", false)):
			count += 1
	return count


## Reading a card. The first star is for getting through all nine.
func mark_seen(who: String, island: int, name_number: int) -> void:
	var state := island_state(who, island).duplicate(true)
	var seen: Array = state.get("seen", [])
	if seen.has(name_number):
		return
	seen.append(name_number)
	state["seen"] = seen
	if seen.size() >= Journey.NAMES_PER_ISLAND:
		state["stars"] = maxi(int(state.get("stars", 0)), 1)
	_write(who, island, state)


## Finishing the game. Three stars for a clean run, two otherwise; a sloppier
## replay never takes back what was already earned.
func finish_island(who: String, island: int, mistakes: int) -> void:
	var state := island_state(who, island).duplicate(true)
	state["stars"] = maxi(int(state.get("stars", 0)), 3 if mistakes == 0 else 2)
	state["done"] = true
	_write(who, island, state)


func _write(who: String, island: int, state: Dictionary) -> void:
	if not _progress.has(who):
		_progress[who] = {}
	_progress[who][str(island)] = state
	save_all()
