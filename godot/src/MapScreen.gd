class_name MapScreen
extends Node3D
## The journey, as a real place: eleven islands in an ocean, seen from above
## and slightly to one side, with the camera sailing from island to island.

signal island_chosen(number: int)

const WORLD_W := 46.0      # how wide the archipelago spreads
const WORLD_L := 168.0     # how far the route runs
const ISLE_R := 3.1        # a size-1 island's radius
const ISLE_H := 1.9        # and how tall it stands

var _camera: Camera3D
var _water_material: ShaderMaterial
var _islands: Dictionary = {}     # island number -> Node3D
var _who: String = ""
var _lang: Journey.Lang = Journey.Lang.BN
var _focus_z: float = 0.0
var _target_z: float = 0.0


static func world_position(isl: Dictionary) -> Vector3:
	return Vector3(
		(float(isl["x"]) - 0.5) * WORLD_W,
		0.0,
		float(isl["y"]) * WORLD_L,
	)


static func island_radius(isl: Dictionary) -> float:
	return ISLE_R * float(isl["size"])


func setup(who: String, lang: Journey.Lang) -> void:
	_who = who
	_lang = lang
	_build_sky()
	_build_water()
	_build_islands()
	_build_route()
	_build_camera()
	_look_at_current(true)


func _build_sky() -> void:
	var env := Environment.new()
	env.background_mode = Environment.BG_SKY

	var sky := Sky.new()
	var mat := ProceduralSkyMaterial.new()
	mat.sky_top_color = Color("9fd9ea")
	mat.sky_horizon_color = Color("fdf1dc")
	mat.ground_bottom_color = Color("bfe6ee")
	mat.ground_horizon_color = Color("fdf1dc")
	mat.sun_angle_max = 40.0
	sky.sky_material = mat
	env.sky = sky

	env.ambient_light_source = Environment.AMBIENT_SOURCE_SKY
	env.ambient_light_energy = 1.15
	env.fog_enabled = true
	env.fog_mode = Environment.FOG_MODE_DEPTH
	env.fog_light_color = Color("e8f4f2")
	env.fog_density = 0.0
	env.fog_depth_begin = 90.0
	env.fog_depth_end = 190.0

	var holder := WorldEnvironment.new()
	holder.environment = env
	add_child(holder)

	# late-afternoon sun, low enough to throw the islands' shadows sideways
	var sun := DirectionalLight3D.new()
	sun.light_color = Color("fff4d8")
	sun.light_energy = 1.25
	sun.shadow_enabled = true
	sun.rotation_degrees = Vector3(-48.0, -38.0, 0.0)
	add_child(sun)


func _build_water() -> void:
	var plane := PlaneMesh.new()
	plane.size = Vector2(WORLD_W * 3.0, WORLD_L + 120.0)
	plane.subdivide_width = 60
	plane.subdivide_depth = 160

	_water_material = ShaderMaterial.new()
	_water_material.shader = load("res://shaders/water.gdshader")
	_water_material.set_shader_parameter("shallow_colour", Palette.WATER_SHALLOW)
	_water_material.set_shader_parameter("deep_colour", Palette.WATER_DEEP)

	var isles: Array[Plane] = []
	var packed: Array = []
	for isl: Dictionary in Journey.islands:
		var p := world_position(isl)
		packed.append(Vector4(p.x, p.z, island_radius(isl), 0.0))
	_water_material.set_shader_parameter("isles", packed)
	_water_material.set_shader_parameter("isle_count", packed.size())

	var sea := MeshInstance3D.new()
	sea.mesh = plane
	sea.material_override = _water_material
	sea.position = Vector3(0.0, 0.0, WORLD_L * 0.5)
	add_child(sea)


func _build_islands() -> void:
	for isl: Dictionary in Journey.islands:
		var number := int(isl["i"])
		var holder := Node3D.new()
		holder.position = world_position(isl)
		add_child(holder)

		var open := Store.unlocked(_who, number)
		var state := Store.island_state(_who, number)

		var body := MeshInstance3D.new()
		body.mesh = IslandMesh.build(isl, island_radius(isl), ISLE_H * float(isl["size"]))
		var mat := StandardMaterial3D.new()
		mat.vertex_color_use_as_albedo = true
		mat.roughness = 0.92
		if not open:
			# not reached yet: drained of colour and sunk into the haze
			mat.albedo_color = Color(0.72, 0.78, 0.82, 1.0)
		body.material_override = mat
		holder.add_child(body)

		if not open:
			holder.position.y = -0.55

		_islands[number] = holder


func _build_route() -> void:
	for k in range(Journey.islands.size() - 1):
		var a := world_position(Journey.islands[k])
		var b := world_position(Journey.islands[k + 1])
		var sailed := bool(Store.island_state(_who, int(Journey.islands[k]["i"])).get("done", false))
		_lay_dashes(a, b, sailed)


## The route is laid as separate markers rather than a line, so it reads as
## stepping stones on the water and needs no line renderer.
func _lay_dashes(from: Vector3, to: Vector3, sailed: bool) -> void:
	var span := from.distance_to(to)
	var steps := int(span / 3.2)
	for k in range(1, steps):
		var t := float(k) / float(steps)
		var p := from.lerp(to, t)
		var dash := MeshInstance3D.new()
		var box := BoxMesh.new()
		box.size = Vector3(1.5, 0.06, 0.55)
		dash.mesh = box
		dash.position = Vector3(p.x, 0.05, p.z)
		dash.look_at_from_position(dash.position, Vector3(to.x, 0.05, to.z), Vector3.UP)
		var mat := StandardMaterial3D.new()
		mat.albedo_color = Palette.APRICOT if sailed else Color(1, 1, 1, 0.75)
		mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
		dash.material_override = mat
		add_child(dash)


func _build_camera() -> void:
	_camera = Camera3D.new()
	_camera.fov = 52.0
	add_child(_camera)


## Where the journey is up to: the first island still to finish.
func current_island() -> Dictionary:
	for isl: Dictionary in Journey.islands:
		var number := int(isl["i"])
		if Store.unlocked(_who, number) and not bool(Store.island_state(_who, number).get("done", false)):
			return isl
	return Journey.islands[-1] if Journey.islands.size() > 0 else {}


func _look_at_current(immediate: bool) -> void:
	var isl := current_island()
	if isl.is_empty():
		return
	_target_z = world_position(isl).z
	if immediate:
		_focus_z = _target_z
		_place_camera()


func _place_camera() -> void:
	# behind and above, tilted down: enough to see the next island or two
	_camera.position = Vector3(0.0, 19.0, _focus_z - 22.0)
	_camera.look_at(Vector3(0.0, 0.0, _focus_z + 7.0), Vector3.UP)


func _process(delta: float) -> void:
	if _camera == null:
		return
	_focus_z = lerpf(_focus_z, _target_z, clampf(delta * 2.2, 0.0, 1.0))
	_place_camera()
