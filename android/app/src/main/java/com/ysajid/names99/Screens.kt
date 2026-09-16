package com.ysajid.names99

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val PageWidth = 560.dp

@Composable
private fun Page(
    state: JourneyState,
    onBack: (() -> Unit)? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar(state.lang, state::chooseLang, onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = PageWidth).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

/* ======================= who is playing ======================= */

@Composable
fun ProfilesScreen(state: JourneyState) {
    val words = state.words
    var typed by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Page(state) {
        Spacer(Modifier.height(40.dp))
        Text("🌙", fontSize = 54.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            words.pickWho,
            color = Palette.ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            words.pickSub,
            color = Palette.inkSoft,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        state.profiles.forEach { name ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 62.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Palette.card)
                        .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(18.dp))
                        .clickable { state.use(name) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        name,
                        color = Palette.ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        words.islandsDone(state.litFor(name), state.journey.count),
                        color = Palette.inkFaint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(18.dp))
                        .clickable { pendingDelete = name },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = Palette.inkFaint, fontSize = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().imePadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x40000000))
                    .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (typed.isEmpty()) {
                    Text(words.newName, color = Palette.inkFaint, fontSize = 18.sp)
                }
                BasicTextField(
                    value = typed,
                    onValueChange = { if (it.length <= Store.MAX_NAME) typed = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Palette.ink, fontSize = 18.sp),
                    cursorBrush = SolidColor(Palette.gold),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        state.addProfile(typed)
                        typed = ""
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GoldButton(words.add) {
                state.addProfile(typed)
                typed = ""
            }
        }
        Spacer(Modifier.height(40.dp))
    }

    pendingDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Palette.night2,
            titleContentColor = Palette.ink,
            textContentColor = Palette.inkSoft,
            title = { Text(words.removeQuestion(name)) },
            confirmButton = {
                TextButton(onClick = {
                    state.removeProfile(name)
                    pendingDelete = null
                }) { Text(words.delete, color = Palette.rose) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(words.cancel, color = Palette.inkSoft)
                }
            },
        )
    }
}

/* ======================= the map ======================= */

@Composable
fun MapScreen(state: JourneyState) {
    val words = state.words
    val lit = state.progress.lit()

    Page(state) {
        Spacer(Modifier.height(4.dp))
        Eyebrow(words.subtitle)
        Spacer(Modifier.height(8.dp))
        Text(
            words.journey,
            color = Palette.ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))

        // how many nights in
        Box(
            Modifier
                .fillMaxWidth(0.72f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x1AFFFFFF)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(lit.toFloat() / state.journey.count)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Palette.goldDeep, Palette.gold)))
            )
        }
        Spacer(Modifier.height(10.dp))
        Eyebrow(words.islandsDone(lit, state.journey.count))
        Spacer(Modifier.height(6.dp))
        state.who?.let { who ->
            Text(
                words.playingAs(who),
                color = Palette.inkSoft,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { state.toProfiles() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        Spacer(Modifier.height(18.dp))

        state.journey.islands.forEach { isl ->
            IslandRow(state, isl)
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(10.dp))
        PlainButton(words.viewAll, Modifier.fillMaxWidth()) { state.toAll() }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun IslandRow(state: JourneyState, isl: Island) {
    val words = state.words
    val open = state.progress.unlocked(isl.i)
    val here = state.progress.of(isl.i)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (open) Palette.islandFill(isl.hue) else Color(0x0AFFFFFF))
            .border(
                BorderStroke(1.dp, if (open) Palette.islandEdge(isl.hue) else Color(0x14FFFFFF)),
                RoundedCornerShape(22.dp),
            )
            .clickable(enabled = open) { state.openIsland(isl.i) }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (open) Palette.islandBadge(isl.hue) else Color(0x0DFFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (open) isl.emoji else "🔒", fontSize = 26.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                words.nth(isl.i),
                color = Palette.inkFaint,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                isl.title(state.lang),
                color = if (open) Palette.ink else Palette.inkFaint,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            if (open) {
                StarRow(here.stars)
            } else {
                Text(words.locked, color = Palette.inkFaint, fontSize = 14.sp)
            }
        }
        if (here.done) {
            Text("✓", color = Palette.green, fontSize = 22.sp)
        }
    }
}

/* ======================= one name ======================= */

@Composable
fun CardScreen(state: JourneyState) {
    val words = state.words
    val entry = state.currentCard()
    val cards = state.cards()
    var playing by remember { mutableStateOf(false) }

    Page(state, onBack = state::toMap) {
        Eyebrow(state.journey.island(state.island).title(state.lang))
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cards.forEachIndexed { index, card ->
                val seen = card.n in state.progress.of(state.island).seen
                Box(
                    Modifier
                        .size(if (index == state.cardAt) 11.dp else 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                index == state.cardAt -> Palette.gold
                                seen -> Palette.goldDeep
                                else -> Color(0x33FFFFFF)
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        GlassCard {
            NameBody(
                entry = entry,
                lang = state.lang,
                header = "${words.nth(state.island)} · ${words.cardCount(state.cardAt + 1, NAMES_PER_ISLAND)}",
                speaker = if (state.voice.has(entry.n)) {
                    {
                        SpeakerButton(words.listen, playing) {
                            playing = true
                            state.voice.play(entry.n) { playing = false }
                        }
                    }
                } else null,
            )
        }

        Spacer(Modifier.height(18.dp))
        TodayBox(words.todayLabel, entry.lines(state.lang).today)
        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlainButton(
                words.back,
                Modifier.weight(1f),
                enabled = state.cardAt > 0,
            ) { state.previousCard() }
            GoldButton(
                if (state.cardAt == NAMES_PER_ISLAND - 1) words.toGame else words.next,
                Modifier.weight(1f),
            ) { state.nextCard() }
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun SpeakerButton(label: String, playing: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (playing) Palette.gold else Color(0x14FFFFFF))
            .border(
                BorderStroke(1.dp, if (playing) Color.Transparent else Palette.cardLine),
                RoundedCornerShape(50),
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("🔊", fontSize = 16.sp)
        Text(
            label,
            color = if (playing) Color(0xFF241A02) else Palette.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ======================= island lit ======================= */

@Composable
fun FinishScreen(state: JourneyState) {
    val words = state.words
    val isl = state.journey.island(state.island)
    val here = state.progress.of(state.island)
    val lastCard = state.cards().last()
    val allDone = state.progress.lit() >= state.journey.count

    Page(state) {
        Spacer(Modifier.height(50.dp))
        Text(isl.emoji, fontSize = 68.sp)
        Spacer(Modifier.height(18.dp))
        StarRow(here.stars, size = 34, spacing = 8)
        Spacer(Modifier.height(18.dp))
        Text(
            words.wellDone,
            color = Palette.ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Eyebrow(isl.title(state.lang))
        Spacer(Modifier.height(18.dp))
        Text(
            if (allDone) words.lastIsland else words.finishSub,
            color = Palette.inkSoft,
            fontSize = 17.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        TodayBox(words.todayLabel, lastCard.lines(state.lang).today)
        Spacer(Modifier.height(26.dp))
        GoldButton(words.backToMap, Modifier.fillMaxWidth()) { state.toMap() }
        Spacer(Modifier.height(40.dp))
    }
}

/* ======================= the full 99 ======================= */

@Composable
fun AllNamesScreen(state: JourneyState) {
    val words = state.words
    var showing by remember { mutableStateOf<NameEntry?>(null) }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar(state.lang, state::chooseLang, onBack = state::toMap)
        Column(
            Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                words.allNames,
                color = Palette.ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Eyebrow(words.allSub)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.journey.inTraditionalOrder, key = { it.n }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.card)
                        .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(16.dp))
                        .clickable { showing = entry }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        entry.n.inLang(state.lang),
                        color = Palette.inkFaint,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(34.dp),
                        textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        entry.arabic,
                        color = Palette.gold,
                        fontSize = 24.sp,
                        modifier = Modifier.width(120.dp),
                        textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.lines(state.lang).meaning,
                            color = Palette.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            entry.translit,
                            color = Palette.inkFaint,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
            }
        }
    }

    showing?.let { entry ->
        Dialog(onDismissRequest = { showing = null }) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Palette.night2)
                    .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(26.dp))
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NameBody(
                    entry = entry,
                    lang = state.lang,
                    header = "${words.nth(entry.island)} · ${words.cardCount(entry.n, 99)}",
                )
                Spacer(Modifier.height(18.dp))
                TodayBox(words.todayLabel, entry.lines(state.lang).today)
                Spacer(Modifier.height(18.dp))
                PlainButton(words.close, Modifier.fillMaxWidth()) { showing = null }
            }
        }
    }
}
