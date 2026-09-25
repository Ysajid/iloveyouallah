shader_type spatial;
// Daylight shallows, written for the GL Compatibility renderer so it runs on
// an old tablet. No depth texture, no screen reads: the shoreline is worked
// out from where the islands are, which is cheaper and easier to keep calm.

render_mode blend_mix, cull_back, diffuse_lambert, specular_schlick_ggx;

uniform vec3 shallow_colour : source_color = vec3(0.75, 0.91, 0.94);
uniform vec3 deep_colour : source_color = vec3(0.25, 0.62, 0.72);
uniform vec3 foam_colour : source_color = vec3(1.0, 1.0, 1.0);

uniform float wave_height = 0.06;
uniform float wave_speed = 0.45;

// xz = island centre, w = radius. Kept short: there are only eleven.
uniform vec4 isles[11];
uniform int isle_count = 11;

varying vec3 world_pos;

float ripple(vec2 p, float t) {
	return sin(p.x * 0.65 + t) * 0.5
		+ sin(p.y * 0.48 - t * 0.8) * 0.3
		+ sin((p.x + p.y) * 0.31 + t * 1.3) * 0.2;
}

void vertex() {
	world_pos = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
	float t = TIME * wave_speed;
	VERTEX.y += ripple(world_pos.xz, t) * wave_height;
}

void fragment() {
	// how close this pixel is to any shore, 0 far out, 1 on the sand
	float nearest = 1e9;
	for (int i = 0; i < isle_count; i++) {
		float d = distance(world_pos.xz, isles[i].xy) - isles[i].z;
		nearest = min(nearest, d);
	}

	float shore = 1.0 - smoothstep(0.0, 1.1, max(nearest, 0.0));
	vec3 base = mix(deep_colour, shallow_colour, shore);

	// a band of foam that breathes in and out against the shore
	float t = TIME * wave_speed;
	float band = 1.0 - smoothstep(0.0, 0.34, abs(nearest - 0.25 - sin(t * 0.9) * 0.09));
	float texture_noise = 0.55 + 0.45 * ripple(world_pos.xz * 2.3, t * 1.4);
	float foam = clamp(band * texture_noise, 0.0, 1.0);

	ALBEDO = mix(base, foam_colour, foam * 0.45);
	ROUGHNESS = mix(0.10, 0.45, foam);
	SPECULAR = 0.2;
}
