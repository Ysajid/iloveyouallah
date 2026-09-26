extends Node
## Boots the journey and switches between screens.
##
## Also honours --shot=<path> --shot-frames=<n>, which renders a few frames and
## writes a PNG. That flag is how this gets checked on a machine with no screen
## and no device: the scene is rendered for real under a virtual display.

var _map: MapScreen
var _ui: CanvasLayer          # 2D screens live here, above the 3D map
var _who: String = ""


func _ready() -> void:
	# Step 0: --proof shows only the text-shaping check, nothing else
	# A Control parented to a plain Node never gets the viewport rect and so
	# draws nothing at all. Everything 2D goes through this layer, which also
	# puts the reading screens above the 3D map where they belong.
	_ui = CanvasLayer.new()
	add_child(_ui)

	# Step 0: --proof shows only the text-shaping check, nothing else
	if "--proof" in OS.get_cmdline_args():
		show_screen(TextProof.new())
		_maybe_screenshot()
		return

	_who = Store.last_profile
	if _who.is_empty():
		# a stand-in so the map has something to draw before profiles exist
		_who = Store.profiles[0] if Store.profiles.size() > 0 else "…"
	_open_map()
	_maybe_screenshot()


## Puts one full-screen Control on the UI layer, replacing whatever was there.
func show_screen(screen: Control) -> void:
	for child in _ui.get_children():
		child.queue_free()
	screen.set_anchors_preset(Control.PRESET_FULL_RECT)
	_ui.add_child(screen)


func _open_map() -> void:
	if _map != null:
		_map.queue_free()
	_map = MapScreen.new()
	add_child(_map)
	_map.setup(_who, Store.lang)
	_map.island_chosen.connect(_on_island_chosen)


func _on_island_chosen(number: int) -> void:
	print("island chosen: ", number)


# --- rendering to a file, for checking without a screen ---

func _cli(flag: String, fallback: String) -> String:
	for arg: String in OS.get_cmdline_user_args() + OS.get_cmdline_args():
		if arg.begins_with(flag + "="):
			return arg.substr(flag.length() + 1)
	return fallback


func _maybe_screenshot() -> void:
	var path := _cli("--shot", "")
	if path.is_empty():
		return
	var frames := int(_cli("--shot-frames", "45"))
	_render_then_quit(path, frames)


func _render_then_quit(path: String, frames: int) -> void:
	# let the water settle and the camera arrive before capturing
	for _i in frames:
		await get_tree().process_frame
	await RenderingServer.frame_post_draw
	var image := get_viewport().get_texture().get_image()
	var err := image.save_png(path)
	print("shot -> ", path, " (", "ok" if err == OK else "error %d" % err, ")")
	get_tree().quit()
