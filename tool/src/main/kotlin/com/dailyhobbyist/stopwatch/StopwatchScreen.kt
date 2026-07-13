package com.dailyhobbyist.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.designVerticalPxToSp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.material3.Text
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter

@InitialScreen
class StopwatchScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, StopwatchViewModel>(sealedActivity) {

    override val viewModelClass: Class<StopwatchViewModel>
        get() = StopwatchViewModel::class.java

    override fun createViewModel(): StopwatchViewModel {
        return StopwatchViewModel(lightContext.dataStore)
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val isRunning by viewModel.isRunning.collectAsState()
        val elapsed by viewModel.elapsedMs.collectAsState()
        val laps by viewModel.laps.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    center = LightTopBarCenter.Text("STOPWATCH"),
                    rightButton = LightBarButton.Text("HISTORY") {
                        navigateTo(screenFactory = { sealed -> HistoryScreen(sealed) })
                    },
                )

                // ---- big time display ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FixedWidthTime(
                        text = formatTime(elapsed),
                        style = LightThemeTokens.typography.subtitle,
                    )
                }

                // ---- laps ----
                LapList(
                    elapsed = elapsed,
                    isRunning = isRunning,
                    laps = laps,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                )

                // ---- controls ----
                val hasTime = elapsed > 0L || laps.isNotEmpty()
                val items: List<LightBarButton?> = when {
                    isRunning -> listOf(
                        LightBarButton.Text("LAP") { viewModel.lap() },
                        LightBarButton.Text("STOP") { viewModel.startStop() },
                    )
                    hasTime -> listOf(
                        LightBarButton.Text("RESET") { viewModel.reset() },
                        LightBarButton.Text("START") { viewModel.startStop() },
                    )
                    else -> listOf(
                        LightBarButton.Text("START") { viewModel.startStop() },
                    )
                }
                LightBottomBar(items = items)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Lap list
// ---------------------------------------------------------------------------

@Composable
private fun LapList(
    elapsed: Long,
    isRunning: Boolean,
    laps: List<Long>,
    modifier: Modifier = Modifier,
) {
    // splits[i] = duration of lap i
    val splits = laps.mapIndexed { i, total ->
        if (i == 0) total else total - laps[i - 1]
    }
    val bestIndex = if (splits.size >= 2) splits.indexOf(splits.min()) else -1
    val slowestIndex = if (splits.size >= 2) splits.indexOf(splits.max()) else -1

    val showLiveLap = isRunning || (laps.isNotEmpty() && elapsed > laps.last())
    val liveSplit = elapsed - (laps.lastOrNull() ?: 0L)

    LazyColumn(modifier = modifier) {
        // live (in-progress) lap at the top
        if (showLiveLap && (laps.isNotEmpty() || elapsed > 0L)) {
            item(key = "live") {
                LapRow(
                    label = "LAP ${laps.size + 1}",
                    tag = null,
                    split = liveSplit,
                    total = elapsed,
                )
            }
        }
        // recorded laps, newest first
        items(
            items = laps.indices.reversed().toList(),
            key = { it },
        ) { i ->
            LapRow(
                label = "LAP ${i + 1}",
                tag = when (i) {
                    bestIndex -> "BEST"
                    slowestIndex -> "SLOWEST"
                    else -> null
                },
                split = splits[i],
                total = laps[i],
            )
        }
    }
}

@Composable
private fun LapRow(
    label: String,
    tag: String?,
    split: Long,
    total: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LightText(
                text = label,
                variant = LightTextVariant.Detail,
            )
            if (tag != null) {
                LightText(
                    text = tag,
                    variant = LightTextVariant.Micro,
                )
            }
        }
        // lap split (the headline number for the row)
        LightText(
            text = formatTime(split),
            variant = LightTextVariant.Detail,
        )
        Spacer(modifier = Modifier.width(18.dp))
        // running total at that lap
        LightText(
            text = formatTime(total),
            variant = LightTextVariant.Fine,
        )
    }
}

// ---------------------------------------------------------------------------
// Fixed-width time text — every digit sits in an equal slot so the display
// doesn't shift around as numbers change (Akkurat digits vary in width).
// ---------------------------------------------------------------------------

@Composable
private fun FixedWidthTime(
    text: String,
    style: TextStyle,
) {
    // Scale the token style the same way LightText does (design px → sp),
    // then measure and render with that one style so slots line up exactly.
    val scaled = style.copy(
        fontSize = style.fontSize.value.designVerticalPxToSp(),
        lineHeight = if (style.lineHeight.isSpecified) {
            style.lineHeight.value.designVerticalPxToSp()
        } else style.lineHeight,
        letterSpacing = if (style.letterSpacing.isSpecified) {
            style.letterSpacing.value.designVerticalPxToSp()
        } else style.letterSpacing,
        color = LightThemeTokens.colors.content,
    )

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val digitWidth = remember(scaled) {
        val w = (0..9).maxOf { d ->
            measurer.measure(d.toString(), scaled).size.width
        }
        with(density) { w.toDp() }
    }
    val sepWidth = remember(scaled) {
        val w = maxOf(
            measurer.measure(":", scaled).size.width,
            measurer.measure(".", scaled).size.width,
        )
        with(density) { w.toDp() }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        text.forEach { ch ->
            val slot = if (ch.isDigit()) digitWidth else sepWidth
            Box(
                modifier = Modifier.width(slot),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ch.toString(),
                    style = scaled,
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Formatting
// ---------------------------------------------------------------------------

/** 00:00.00 → MM:SS.hh, growing to H:MM:SS.hh past an hour. */
internal fun formatTime(ms: Long): String {
    val clamped = if (ms < 0) 0L else ms
    val h = clamped / 3_600_000
    val m = (clamped % 3_600_000) / 60_000
    val s = (clamped % 60_000) / 1_000
    val c = (clamped % 1_000) / 10
    return if (h > 0) {
        "%d:%02d:%02d.%02d".format(h, m, s, c)
    } else {
        "%02d:%02d.%02d".format(m, s, c)
    }
}
