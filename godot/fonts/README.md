# Fonts

Godot ships no Bengali or Arabic glyphs, and unlike Android there is no system
font to fall back on — without these files every name renders as tofu.

| File | Family | Used for |
| --- | --- | --- |
| `NotoSansBengali-Regular.ttf`, `-Bold.ttf` | Noto Sans Bengali | all Bangla |
| `NotoNaskhArabic-Regular.ttf`, `-Bold.ttf` | Noto Naskh Arabic | the 99 names |

Both are from the Noto project and are licensed under the
[SIL Open Font License 1.1](https://openfontlicense.org), which allows bundling
them in an application. Fetched from Google Fonts.

Coverage was checked rather than assumed: every character of `মায়ার দ্বীপ` and
`الرَّحْمَٰنُ` is present in each font's character map.

Shaping — joining Arabic letters, and forming Bangla conjuncts like ক্ষ and ন্দ —
is done by Godot's TextServer Advanced, which is why `project.godot` sets
`locale/include_text_server_data=true`. Without that the glyphs are present but
sit apart from each other.
