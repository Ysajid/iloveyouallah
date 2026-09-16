package com.ysajid.names99

import android.content.Context
import org.json.JSONObject

/** Nine names to an island, eleven islands, 99 names. */
const val NAMES_PER_ISLAND = 9

/** Which language the child is reading in. Bangla is the default. */
enum class Lang { BN, EN }

/** The three lines that get rewritten: what it means, a bit about it, one thing to do. */
data class Lines(val meaning: String, val about: String, val today: String)

data class NameEntry(
    /** Position in the traditional order, 1..99. */
    val n: Int,
    /** Which island it sits on, 1..11. */
    val island: Int,
    val arabic: String,
    val translit: String,
    private val bn: Lines,
    private val en: Lines,
) {
    fun lines(lang: Lang): Lines = if (lang == Lang.BN) bn else en
}

data class Island(
    val i: Int,
    val emoji: String,
    private val bn: String,
    private val en: String,
    val hue: Float,
) {
    fun title(lang: Lang): String = if (lang == Lang.BN) bn else en
}

/** The whole journey, read once from assets at startup. */
class Journey(val islands: List<Island>, val names: List<NameEntry>) {

    private val byIsland: Map<Int, List<NameEntry>> = names.groupBy { it.island }

    val inTraditionalOrder: List<NameEntry> = names.sortedBy { it.n }

    fun namesOf(island: Int): List<NameEntry> = byIsland[island].orEmpty()

    fun island(i: Int): Island = islands.first { it.i == i }

    val count: Int get() = islands.size

    companion object {
        fun load(context: Context): Journey =
            parse(context.assets.open("names.json").use { it.readBytes().toString(Charsets.UTF_8) })

        /** Split out from [load] so the data can be checked without a device. */
        fun parse(raw: String): Journey {
            val root = JSONObject(raw)

            val islandsJson = root.getJSONArray("islands")
            val islands = (0 until islandsJson.length()).map { k ->
                val o = islandsJson.getJSONObject(k)
                Island(
                    i = o.getInt("i"),
                    emoji = o.getString("emoji"),
                    bn = o.getString("bn"),
                    en = o.getString("en"),
                    hue = o.getDouble("hue").toFloat(),
                )
            }

            val namesJson = root.getJSONArray("names")
            val names = (0 until namesJson.length()).map { k ->
                val o = namesJson.getJSONObject(k)
                NameEntry(
                    n = o.getInt("n"),
                    island = o.getInt("i"),
                    arabic = o.getString("ar"),
                    translit = o.getString("tr"),
                    bn = o.getJSONObject("bn").toLines(),
                    en = o.getJSONObject("en").toLines(),
                )
            }

            return Journey(islands, names)
        }

        private fun JSONObject.toLines() = Lines(
            meaning = getString("meaning"),
            about = getString("about"),
            today = getString("today"),
        )
    }
}

private val BN_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

/** Numerals the child can actually read: Bangla digits in Bangla, Western in English. */
fun Int.inLang(lang: Lang): String {
    val plain = this.toString()
    if (lang != Lang.BN) return plain
    return buildString { plain.forEach { append(BN_DIGITS[it - '0']) } }
}
