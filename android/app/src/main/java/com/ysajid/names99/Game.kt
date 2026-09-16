package com.ysajid.names99

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay

/** Three pairs at a time — nine in one go is too much for a six-year-old. */
private const val PAIRS_PER_ROUND = 3

private enum class TileState { Idle, Picked, Right, Wrong }

@Composable
fun GameScreen(state: JourneyState) {
    val words = state.words

    // one shuffle per visit to the island
    val rounds = remember(state.island) {
        state.cards().shuffled().chunked(PAIRS_PER_ROUND)
    }

    var at by remember(state.island) { mutableIntStateOf(0) }
    var mistakes by remember(state.island) { mutableIntStateOf(0) }
    var pickedName by remember(state.island) { mutableStateOf<Int?>(null) }
    var pickedMeaning by remember(state.island) { mutableStateOf<Int?>(null) }
    var solved by remember(state.island) { mutableStateOf(emptySet<Int>()) }
    var wrong by remember(state.island) { mutableStateOf(emptySet<Int>()) }

    val round = rounds[at]
    val meanings = remember(state.island, at) { round.shuffled() }

    // a wrong pair flashes, then clears itself
    LaunchedEffect(wrong) {
        if (wrong.isNotEmpty()) {
            delay(420)
            wrong = emptySet()
            pickedName = null
            pickedMeaning = null
        }
    }

    // round finished: pause on the green, then move on
    LaunchedEffect(solved, at) {
        if (solved.size == round.size) {
            delay(450)
            if (at < rounds.lastIndex) {
                at += 1
                solved = emptySet()
                pickedName = null
                pickedMeaning = null
            } else {
                state.finishGame(mistakes)
            }
        }
    }

    fun judge() {
        val a = pickedName ?: return
        val b = pickedMeaning ?: return
        if (a == b) {
            solved = solved + a
            pickedName = null
            pickedMeaning = null
        } else {
            mistakes += 1
            wrong = setOf(a, b)
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar(state.lang, state::chooseLang, onBack = state::toMap)

        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 560.dp).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Eyebrow(words.round(at + 1, rounds.size))
                Spacer(Modifier.height(8.dp))
                Text(
                    words.gameTitle,
                    color = Palette.ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    words.gameSub,
                    color = Palette.inkSoft,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        round.forEach { entry ->
                            Tile(
                                text = entry.arabic,
                                arabic = true,
                                tile = tileState(entry.n, solved, wrong, pickedName),
                            ) {
                                if (entry.n !in solved && wrong.isEmpty()) {
                                    pickedName = if (pickedName == entry.n) null else entry.n
                                    judge()
                                }
                            }
                        }
                    }
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        meanings.forEach { entry ->
                            Tile(
                                text = entry.lines(state.lang).meaning,
                                arabic = false,
                                tile = tileState(entry.n, solved, wrong, pickedMeaning),
                            ) {
                                if (entry.n !in solved && wrong.isEmpty()) {
                                    pickedMeaning = if (pickedMeaning == entry.n) null else entry.n
                                    judge()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tileState(
    n: Int,
    solved: Set<Int>,
    wrong: Set<Int>,
    picked: Int?,
): TileState = when {
    n in solved -> TileState.Right
    n in wrong -> TileState.Wrong
    n == picked -> TileState.Picked
    else -> TileState.Idle
}

@Composable
private fun Tile(
    text: String,
    arabic: Boolean,
    tile: TileState,
    onClick: () -> Unit,
) {
    val edge by animateColorAsState(
        when (tile) {
            TileState.Right -> Palette.green
            TileState.Wrong -> Palette.rose
            TileState.Picked -> Palette.gold
            TileState.Idle -> Palette.cardLine
        },
        label = "edge",
    )
    val fill by animateColorAsState(
        when (tile) {
            TileState.Right -> Palette.green.copy(alpha = 0.16f)
            TileState.Wrong -> Palette.rose.copy(alpha = 0.14f)
            TileState.Picked -> Palette.gold.copy(alpha = 0.16f)
            TileState.Idle -> Palette.card
        },
        label = "fill",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .alpha(if (tile == TileState.Right) 0.55f else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .border(BorderStroke(2.dp, edge), RoundedCornerShape(20.dp))
            .clickable(enabled = tile != TileState.Right) { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (arabic) Palette.gold else Palette.ink,
            fontSize = if (arabic) 28.sp else 16.sp,
            lineHeight = if (arabic) 44.sp else 24.sp,
            fontWeight = if (arabic) FontWeight.Normal else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
