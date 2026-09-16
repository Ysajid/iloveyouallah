package com.ysajid.names99

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** What one child has done on one island. */
data class IslandProgress(
    val seen: Set<Int> = emptySet(),
    val stars: Int = 0,
    val done: Boolean = false,
)

/** What one child has done overall. Immutable: every change makes a new copy and saves. */
data class Progress(val islands: Map<Int, IslandProgress> = emptyMap()) {

    fun of(island: Int): IslandProgress = islands[island] ?: IslandProgress()

    /** Island one is always open; the rest wait for the night before. */
    fun unlocked(island: Int): Boolean = island == 1 || of(island - 1).done

    fun lit(): Int = islands.values.count { it.done }

    fun with(island: Int, change: (IslandProgress) -> IslandProgress): Progress =
        Progress(islands + (island to change(of(island))))

    /**
     * Reading a card. The first star is for getting through all nine of them.
     * Re-reading a card already seen changes nothing.
     */
    fun seeing(island: Int, name: Int, perIsland: Int = NAMES_PER_ISLAND): Progress {
        if (name in of(island).seen) return this
        return with(island) { before ->
            val seen = before.seen + name
            before.copy(
                seen = seen,
                stars = if (seen.size >= perIsland) maxOf(before.stars, 1) else before.stars,
            )
        }
    }

    /**
     * Finishing the matching game. Two stars for getting through it, three for
     * getting through it without a wrong match. Stars never go down: a sloppier
     * replay cannot take away what was already earned.
     */
    fun finishing(island: Int, mistakes: Int): Progress {
        val earned = if (mistakes == 0) 3 else 2
        return with(island) { it.copy(stars = maxOf(it.stars, earned), done = true) }
    }

    fun toJson(): String {
        val root = JSONObject()
        islands.forEach { (island, p) ->
            root.put(
                island.toString(),
                JSONObject()
                    .put("seen", JSONArray(p.seen.toList()))
                    .put("stars", p.stars)
                    .put("done", p.done),
            )
        }
        return root.toString()
    }

    companion object {
        fun fromJson(raw: String?): Progress {
            if (raw.isNullOrBlank()) return Progress()
            return try {
                val root = JSONObject(raw)
                val out = mutableMapOf<Int, IslandProgress>()
                root.keys().forEach { key ->
                    val o = root.getJSONObject(key)
                    val seenJson = o.optJSONArray("seen") ?: JSONArray()
                    out[key.toInt()] = IslandProgress(
                        seen = (0 until seenJson.length()).map { seenJson.getInt(it) }.toSet(),
                        stars = o.optInt("stars", 0),
                        done = o.optBoolean("done", false),
                    )
                }
                Progress(out)
            } catch (e: Exception) {
                // A corrupt entry should cost one child their stars, not crash bedtime.
                Progress()
            }
        }
    }
}

/**
 * Everything that has to survive being closed: who plays, how far each of them
 * got, and which language they last read in.
 */
class Store(context: Context) {

    private val prefs = context.getSharedPreferences("nj99", Context.MODE_PRIVATE)

    var lang: Lang
        get() = if (prefs.getString(KEY_LANG, "bn") == "en") Lang.EN else Lang.BN
        set(value) = prefs.edit().putString(KEY_LANG, if (value == Lang.EN) "en" else "bn").apply()

    var lastProfile: String?
        get() = prefs.getString(KEY_LAST, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_LAST) else putString(KEY_LAST, value)
        }.apply()

    fun profiles(): List<String> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addProfile(name: String) {
        val clean = name.trim().take(MAX_NAME)
        if (clean.isEmpty()) return
        val list = profiles()
        if (clean in list) return
        prefs.edit().putString(KEY_PROFILES, JSONArray(list + clean).toString()).apply()
    }

    fun removeProfile(name: String) {
        prefs.edit()
            .putString(KEY_PROFILES, JSONArray(profiles() - name).toString())
            .remove(progressKey(name))
            .apply()
        if (lastProfile == name) lastProfile = null
    }

    fun progress(who: String): Progress =
        Progress.fromJson(prefs.getString(progressKey(who), null))

    fun save(who: String, progress: Progress) {
        prefs.edit().putString(progressKey(who), progress.toJson()).apply()
    }

    private fun progressKey(who: String) = "p.$who"

    companion object {
        const val MAX_NAME = 24
        private const val KEY_LANG = "lang"
        private const val KEY_LAST = "last"
        private const val KEY_PROFILES = "profiles"
    }
}
