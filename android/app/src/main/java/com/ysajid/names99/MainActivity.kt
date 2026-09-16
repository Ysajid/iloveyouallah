package com.ysajid.names99

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class Screen { Profiles, Map, Card, Game, Finish, All }

/**
 * One object holding everything the screens read and write. Progress is saved
 * the moment it changes, so closing the app mid-island never loses a night.
 */
class JourneyState(
    private val store: Store,
    val journey: Journey,
    val voice: Voice,
) {
    var lang by mutableStateOf(store.lang)
        private set
    var profiles by mutableStateOf(store.profiles())
        private set
    var who by mutableStateOf<String?>(null)
        private set
    var progress by mutableStateOf(Progress())
        private set
    var screen by mutableStateOf(Screen.Profiles)
        private set
    var island by mutableStateOf(1)
        private set
    var cardAt by mutableStateOf(0)
        private set

    val words: Words get() = Words(lang)

    fun chooseLang(value: Lang) {
        lang = value
        store.lang = value
    }

    // ---- who is playing ----

    fun resume() {
        val last = store.lastProfile
        if (last != null && last in profiles) use(last) else screen = Screen.Profiles
    }

    fun addProfile(name: String) {
        val clean = name.trim().take(Store.MAX_NAME)
        if (clean.isEmpty()) return
        store.addProfile(clean)
        profiles = store.profiles()
        use(clean)
    }

    fun removeProfile(name: String) {
        store.removeProfile(name)
        profiles = store.profiles()
        if (who == name) {
            who = null
            progress = Progress()
        }
    }

    fun use(name: String) {
        who = name
        store.lastProfile = name
        progress = store.progress(name)
        screen = Screen.Map
    }

    fun litFor(name: String): Int = store.progress(name).lit()

    // ---- moving around ----

    fun toProfiles() { screen = Screen.Profiles }
    fun toMap() { voice.stop(); screen = Screen.Map }
    fun toAll() { screen = Screen.All }

    fun openIsland(i: Int) {
        island = i
        val seen = progress.of(i).seen.size
        // pick up at the first card they have not read, or start over once finished
        cardAt = if (seen >= NAMES_PER_ISLAND) 0 else minOf(seen, NAMES_PER_ISLAND - 1)
        screen = Screen.Card
        markSeen()
    }

    fun cards(): List<NameEntry> = journey.namesOf(island)

    fun currentCard(): NameEntry = cards()[cardAt]

    fun nextCard() {
        voice.stop()
        if (cardAt < NAMES_PER_ISLAND - 1) {
            cardAt++
            markSeen()
        } else {
            screen = Screen.Game
        }
    }

    fun previousCard() {
        voice.stop()
        if (cardAt > 0) cardAt--
    }

    private fun markSeen() {
        val updated = progress.seeing(island, currentCard().n)
        if (updated === progress) return
        progress = updated
        save()
    }

    fun finishGame(mistakes: Int) {
        progress = progress.finishing(island, mistakes)
        save()
        screen = Screen.Finish
    }

    private fun save() {
        who?.let { store.save(it, progress) }
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var voice: Voice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // light bars: the app is cream now, so the system icons must be dark
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK,
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK,
            ),
        )

        val store = Store(applicationContext)
        val journey = Journey.load(applicationContext)
        voice = Voice(applicationContext)

        setContent {
            val state = remember { JourneyState(store, journey, voice).also { it.resume() } }
            App(state) { finish() }
        }
    }

    override fun onPause() {
        super.onPause()
        voice.stop()
    }
}

@Composable
private fun App(state: JourneyState, onExit: () -> Unit) {
    BackHandler {
        when (state.screen) {
            Screen.Profiles -> onExit()
            Screen.Map -> onExit()
            Screen.Card, Screen.Game, Screen.Finish, Screen.All -> state.toMap()
        }
    }

    DayBackdrop {
        when (state.screen) {
            Screen.Profiles -> ProfilesScreen(state)
            Screen.Map -> MapScreen(state)
            Screen.Card -> CardScreen(state)
            Screen.Game -> GameScreen(state)
            Screen.Finish -> FinishScreen(state)
            Screen.All -> AllNamesScreen(state)
        }
    }
}
