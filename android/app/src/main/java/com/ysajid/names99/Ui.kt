package com.ysajid.names99

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The smallest thing a six-year-old can reliably hit. */
val TapSize = 56.dp

@Composable
fun GoldButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .defaultMinSize(minHeight = TapSize)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(listOf(Palette.gold, Palette.goldDeep))
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color(0xFF241A02),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PlainButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .defaultMinSize(minHeight = TapSize)
            .clip(RoundedCornerShape(50))
            .background(Palette.card)
            .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(50))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) Palette.ink else Palette.inkFaint,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun IconSquare(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.card)
            .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = Palette.ink, fontSize = 20.sp)
    }
}

@Composable
fun LangToggle(lang: Lang, onChange: (Lang) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.card)
            .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(50)),
    ) {
        listOf(Lang.BN to "বাংলা", Lang.EN to "EN").forEach { (value, label) ->
            val on = value == lang
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Palette.gold else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            ) {
                Text(
                    label,
                    color = if (on) Color(0xFF241A02) else Palette.inkFaint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
fun TopBar(lang: Lang, onLang: (Lang) -> Unit, onBack: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconSquare("←", onBack) else Spacer(Modifier.width(46.dp))
        Spacer(Modifier.weight(1f))
        LangToggle(lang, onLang)
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = Palette.inkFaint,
        fontSize = 12.sp,
        letterSpacing = 1.8.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun StarRow(filled: Int, size: Int = 18, spacing: Int = 4) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
        repeat(3) { k ->
            Text(
                "★",
                color = if (k < filled) Palette.gold else Palette.gold.copy(alpha = 0.22f),
                fontSize = size.sp,
            )
        }
    }
}

/** The line I read at bedtime. It gets its own box on every screen it appears. */
@Composable
fun TodayBox(label: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Palette.gold.copy(alpha = 0.17f), Palette.gold.copy(alpha = 0.06f))
                )
            )
            .border(
                BorderStroke(1.dp, Palette.gold.copy(alpha = 0.30f)),
                RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            label.uppercase(),
            color = Palette.gold,
            fontSize = 12.sp,
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            color = Palette.ink,
            fontSize = 20.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Arabic, transliteration, meaning and the explanation — the body of a card. */
@Composable
fun NameBody(
    entry: NameEntry,
    lang: Lang,
    header: String,
    speaker: (@Composable () -> Unit)? = null,
) {
    val lines = entry.lines(lang)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow(header)
        Spacer(Modifier.height(10.dp))
        Text(
            entry.arabic,
            color = Palette.gold,
            fontSize = 52.sp,
            lineHeight = 78.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            entry.translit,
            color = Palette.inkSoft,
            fontSize = 17.sp,
            fontStyle = FontStyle.Italic,
        )
        if (speaker != null) {
            Spacer(Modifier.height(16.dp))
            speaker()
        }
        Spacer(Modifier.height(20.dp))
        Text(
            lines.meaning,
            color = Palette.ink,
            fontSize = 24.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Palette.cardLine)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            lines.about,
            color = Palette.ink,
            fontSize = 18.sp,
            lineHeight = 31.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** The translucent panel most content sits on. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 28.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x1CFFFFFF), Color(0x0BFFFFFF))
                )
            )
            .border(BorderStroke(1.dp, Palette.cardLine), RoundedCornerShape(26.dp))
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
