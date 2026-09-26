class_name TextProof
extends Control
## Step 0: does Godot actually shape Bangla and Arabic?
##
## Bundling a font only proves the glyphs exist. What matters is shaping —
## whether ক + ্ + ষ becomes the single conjunct ক্ষ, and whether Arabic letters
## join and carry their diacritics. That is TextServer Advanced's job, using the
## ICU data enabled by locale/include_text_server_data.
##
## This screen exists to be looked at, on a desktop render and then on a real
## tablet, because the editor and an exported build load that data differently.
## If the conjuncts break here, the Godot rebuild stops here too.

const BENGALI := "res://fonts/NotoSansBengali-Regular.ttf"
const BENGALI_BOLD := "res://fonts/NotoSansBengali-Bold.ttf"
const ARABIC := "res://fonts/NotoNaskhArabic-Regular.ttf"

## The conjuncts that actually appear in the app's own island names, plus the
## ones that break first in a bad text stack.
const CONJUNCTS := [
	["ক্ষ", "ka + virama + ssa — in ক্ষমার দ্বীপ"],
	["জ্ঞ", "ja + virama + nya — in জ্ঞানের দ্বীপ"],
	["ষ্ট", "ssa + virama + tta — in সৃষ্টির দ্বীপ"],
	["দ্বী", "da + virama + ba + ii — in every দ্বীপ"],
	["ন্দ", "na + virama + da"],
	["হ্ম", "ha + virama + ma"],
	["শ্রী", "sha + virama + ra + ii"],
	["ঙ্গ", "nga + virama + ga"],
]

const ISLAND_NAMES := [
	"মায়ার দ্বীপ", "ক্ষমার দ্বীপ", "সৃষ্টির দ্বীপ",
	"জ্ঞানের দ্বীপ", "চিরকালের দ্বীপ", "মহিমার দ্বীপ",
]

const ACTION_LINES := [
	"আজকে একটা পাখি বা পিঁপড়াকে একটু খাবার দাও।",
	"আল্লাহর দয়া সবার জন্য। ভালো মানুষ, দুষ্টু মানুষ, পাখি, পিঁপড়া — সবার জন্য।",
]

const ARABIC_NAMES := [
	"الرَّحْمَٰنُ",
	"ذُو الْجَلَالِ وَالْإِكْرَامِ",
	"الْمُتَكَبِّرُ",
	"الصَّمَدُ",
]

var _bn: FontFile
var _bn_bold: FontFile
var _ar: FontFile


func _ready() -> void:
	_bn = load(BENGALI)
	_bn_bold = load(BENGALI_BOLD)
	_ar = load(ARABIC)

	var bg := ColorRect.new()
	bg.color = Palette.PAPER
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(bg)

	var scroll := ScrollContainer.new()
	scroll.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(scroll)

	var column := VBoxContainer.new()
	column.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	column.add_theme_constant_override("separation", 10)
	scroll.add_child(column)

	var pad := MarginContainer.new()
	pad.add_theme_constant_override("margin_left", 22)
	pad.add_theme_constant_override("margin_right", 22)
	pad.add_theme_constant_override("margin_top", 20)
	pad.add_theme_constant_override("margin_bottom", 28)
	pad.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.remove_child(column)
	pad.add_child(column)
	scroll.add_child(pad)

	_heading(column, "১ · যুক্তাক্ষর  conjuncts")
	for pair: Array in CONJUNCTS:
		_conjunct_row(column, pair[0], pair[1])

	_heading(column, "২ · island names")
	for name: String in ISLAND_NAMES:
		_line(column, name, _bn_bold, 30, Palette.INK)

	_heading(column, "৩ · a line read at bedtime")
	for line: String in ACTION_LINES:
		_line(column, line, _bn, 21, Palette.INK_SOFT, true)

	_heading(column, "৪ · digits ০১২৩৪৫৬৭৮৯  ·  ৯৯ / ১১")
	_line(column, "৯৯ নামের যাত্রা — ৪ / ১১ দ্বীপ শেষ", _bn_bold, 26, Palette.INK)

	_heading(column, "৫ · Arabic: joining and diacritics")
	for name: String in ARABIC_NAMES:
		_line(column, name, _ar, 40, Palette.TEAL)

	_heading(column, "৬ · both scripts on one line")
	var mixed := _line(column, "الرَّحْمَٰنُ — সবচেয়ে বেশি দয়ালু", _ar, 26, Palette.INK)
	# Bangla falls back to the Bengali face when the Arabic one has no glyph
	var fallback := _ar.duplicate() as FontFile
	fallback.fallbacks = [_bn]
	mixed.add_theme_font_override("font", fallback)


func _heading(parent: Node, text: String) -> void:
	var spacer := Control.new()
	spacer.custom_minimum_size = Vector2(0, 14)
	parent.add_child(spacer)
	var label := Label.new()
	label.text = text
	label.add_theme_font_override("font", _bn_bold)
	label.add_theme_font_size_override("font_size", 16)
	label.add_theme_color_override("font_color", Palette.APRICOT)
	parent.add_child(label)


## The conjunct large on the left, what it is made of on the right. If shaping
## fails, the left shows two or three separate letters with a visible hasant.
func _conjunct_row(parent: Node, glyph: String, note: String) -> void:
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 16)

	var big := Label.new()
	big.text = glyph
	big.add_theme_font_override("font", _bn_bold)
	big.add_theme_font_size_override("font_size", 44)
	big.add_theme_color_override("font_color", Palette.INK)
	big.custom_minimum_size = Vector2(110, 0)
	row.add_child(big)

	var caption := Label.new()
	caption.text = note
	caption.add_theme_font_override("font", _bn)
	caption.add_theme_font_size_override("font_size", 14)
	caption.add_theme_color_override("font_color", Palette.INK_FAINT)
	caption.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	row.add_child(caption)

	parent.add_child(row)


func _line(
	parent: Node, text: String, font: FontFile, size: int,
	colour: Color, wrap: bool = false,
) -> Label:
	var label := Label.new()
	label.text = text
	label.add_theme_font_override("font", font)
	label.add_theme_font_size_override("font_size", size)
	label.add_theme_color_override("font_color", colour)
	if wrap:
		label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		label.custom_minimum_size = Vector2(540, 0)
	parent.add_child(label)
	return label
