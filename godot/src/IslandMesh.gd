class_name IslandMesh
extends RefCounted
## Builds one island as an actual solid: an irregular coastline, a domed top,
## a beach ring and a cliff wall dropping into the water.
##
## Generated rather than modelled, so eleven islands cost no art budget and
## every build draws the same archipelago. Flat-shaded on purpose — the facets
## are what make it read as a toy rather than a blob.

const SEGMENTS := 48      # around the island
const RINGS := 10         # centre out to the shore
const SKIRT_DROP := 1.35  # how far the cliff falls below the waterline


## An irregular but repeatable outline: a circle plus a few harmonics, with the
## island's own seed deciding the phases.
static func _radius_at(angle: float, seed_value: int) -> float:
	var rng := seed_value * 1.7
	return 1.0 \
		+ 0.11 * sin(angle * 2.0 + rng) \
		+ 0.07 * sin(angle * 3.0 - rng * 1.3) \
		+ 0.035 * sin(angle * 5.0 + rng * 0.7)


## Height above the waterline, as a fraction of the island radius.
static func _height_at(t: float) -> float:
	# a plateau with a rounded shoulder falling to a flat beach — a cone reads
	# as a pimple, and these are meant to look like somewhere you could stand
	var d := clampf(t, 0.0, 1.0)
	var plateau := 1.0 - smoothstep(0.34, 0.95, d)
	var dome := 1.0 - 0.45 * d * d
	return plateau * dome


static func build(isl: Dictionary, radius: float, height: float) -> ArrayMesh:
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)

	var seed_value := int(isl["seed"])
	var top := Palette.island_colour(isl, 8.0)
	var mid := Palette.island_colour(isl, 0.0)
	var low := Palette.island_colour(isl, -10.0, 4.0)
	var cliff := Palette.island_colour(isl, -26.0, 6.0)

	# --- the cap, as a proper fan from a single apex ---
	var apex := Vector3(0.0, _height_at(0.0) * height, 0.0)
	var t_first := 1.0 / float(RINGS)
	for seg in SEGMENTS:
		var a0 := TAU * float(seg) / float(SEGMENTS)
		var a1 := TAU * float(seg + 1) / float(SEGMENTS)
		var r0 := _radius_at(a0, seed_value) * radius * t_first
		var r1 := _radius_at(a1, seed_value) * radius * t_first
		var y := _height_at(t_first) * height
		_tri(
			st,
			apex,
			Vector3(cos(a0) * r0, y, sin(a0) * r0),
			Vector3(cos(a1) * r1, y, sin(a1) * r1),
			top, top, top,
		)

	# --- the rest of the top surface, ring by ring ---
	for ring in range(1, RINGS):
		var t0 := float(ring) / float(RINGS)
		var t1 := float(ring + 1) / float(RINGS)
		for seg in SEGMENTS:
			var a0 := TAU * float(seg) / float(SEGMENTS)
			var a1 := TAU * float(seg + 1) / float(SEGMENTS)
			var r0 := _radius_at(a0, seed_value) * radius
			var r1 := _radius_at(a1, seed_value) * radius

			var p00 := Vector3(cos(a0) * r0 * t0, _height_at(t0) * height, sin(a0) * r0 * t0)
			var p01 := Vector3(cos(a1) * r1 * t0, _height_at(t0) * height, sin(a1) * r1 * t0)
			var p10 := Vector3(cos(a0) * r0 * t1, _height_at(t1) * height, sin(a0) * r0 * t1)
			var p11 := Vector3(cos(a1) * r1 * t1, _height_at(t1) * height, sin(a1) * r1 * t1)

			# sand at the waterline, the island's own colour inland
			var c_inner := _shade_at(t0, top, mid)
			var c_outer := _shade_at(t1, top, mid)

			_tri(st, p00, p10, p11, c_inner, c_outer, c_outer)
			_tri(st, p00, p11, p01, c_inner, c_outer, c_inner)

	# --- the cliff, dropping from the shore into the water ---
	for seg in SEGMENTS:
		var a0 := TAU * float(seg) / float(SEGMENTS)
		var a1 := TAU * float(seg + 1) / float(SEGMENTS)
		var r0 := _radius_at(a0, seed_value) * radius
		var r1 := _radius_at(a1, seed_value) * radius

		var s0 := Vector3(cos(a0) * r0, 0.0, sin(a0) * r0)
		var s1 := Vector3(cos(a1) * r1, 0.0, sin(a1) * r1)
		# pulled inward as it drops, so the island sits in the water
		var b0 := Vector3(cos(a0) * r0 * 0.82, -SKIRT_DROP, sin(a0) * r0 * 0.82)
		var b1 := Vector3(cos(a1) * r1 * 0.82, -SKIRT_DROP, sin(a1) * r1 * 0.82)

		_tri(st, s0, b0, b1, Palette.BEACH, cliff, cliff)
		_tri(st, s0, b1, s1, Palette.BEACH, cliff, Palette.BEACH)

	st.generate_normals()
	return st.commit()


## Island colour inland, a clear band of sand at the waterline.
static func _shade_at(t: float, top: Color, mid: Color) -> Color:
	if t < 0.82:
		return top.lerp(mid, t / 0.82)
	return mid.lerp(Palette.BEACH, (t - 0.82) / 0.18)


## One flat-shaded triangle. Normals come from the face, never averaged.
static func _tri(
	st: SurfaceTool,
	a: Vector3, b: Vector3, c: Vector3,
	ca: Color, cb: Color, cc: Color,
) -> void:
	st.set_smooth_group(-1)
	st.set_color(ca); st.add_vertex(a)
	st.set_smooth_group(-1)
	st.set_color(cb); st.add_vertex(b)
	st.set_smooth_group(-1)
	st.set_color(cc); st.add_vertex(c)
