package com.dailyhobbyist.stopwatch

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightLazyScrollView
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

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

        val haptics = LocalHapticFeedback.current
        val focusRequester = remember { FocusRequester() }
        fun haptic() {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        fun hapticTick() {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .focusRequester(focusRequester)
                    .focusable()
                    // External controls: volume rocker drives the watch
                    // (down = start/stop, up = lap). Keys are consumed so
                    // they never adjust the ringer.
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.VolumeDown -> {
                                haptic()
                                viewModel.startStop()
                                true
                            }
                            Key.VolumeUp -> {
                                if (isRunning) {
                                    haptic()
                                    viewModel.lap()
                                }
                                true
                            }
                            else -> false
                        }
                    },
            ) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                LightTopBar(
                    rightButton = LightBarButton.Text("HISTORY") {
                        navigateTo(screenFactory = { sealed -> HistoryScreen(sealed) })
                    },
                )

                // ---- big time display, centred like the LightOS Timer ----
                // Tap = start/stop instantly; a second tap within the
                // double-tap window re-interprets the pair as a lap (the
                // first tap's toggle is reverted). detectTapGestures'
                // onDoubleTap would delay every tap by its 300 ms timeout.
                val gestureState = remember { mutableLongStateOf(0L) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val now = SystemClock.elapsedRealtime()
                                    val lastTapAt = gestureState.longValue and 0xFFFFFFFFL
                                    val lastToggled = (gestureState.longValue shr 32).toInt()
                                    if (now - lastTapAt < 300L &&
                                        viewModel.isRunning.value != (lastToggled == 1)
                                    ) {
                                        // second tap of a double: undo the first
                                        // tap's toggle, then lap
                                        viewModel.startStop()
                                        gestureState.longValue = 0L
                                        hapticTick()
                                        viewModel.lap()
                                    } else {
                                        val wasRunning = viewModel.isRunning.value
                                        gestureState.longValue =
                                            (if (wasRunning) 1L shl 32 else 0L) or
                                                (now and 0xFFFFFFFFL)
                                        haptic()
                                        viewModel.startStop()
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    FixedWidthTime(
                        text = formatTime(elapsed),
                        style = LightThemeTokens.typography.title,
                    )
                }

                // ---- laps: exactly three rows visible, scrollbar only past that ----
                LapList(
                    elapsed = elapsed,
                    isRunning = isRunning,
                    laps = laps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8f.gridUnitsAsDp()),
                )

                // ---- controls: RESET left · START/STOP centre · LAP right ----
                val hasTime = elapsed > 0L || laps.isNotEmpty()
                val items: List<LightBarButton?> = when {
                    isRunning -> listOf(
                        LightBarButton.Text("RESET") { haptic(); viewModel.reset() },
                        LightBarButton.Text("STOP") { haptic(); viewModel.startStop() },
                        LightBarButton.Text("LAP") { hapticTick(); viewModel.lap() },
                    )
                    hasTime -> listOf(
                        LightBarButton.Text("RESET") { haptic(); viewModel.reset() },
                        LightBarButton.Text("START") { haptic(); viewModel.startStop() },
                        null,
                    )
                    else -> listOf(
                        null,
                        LightBarButton.Text("START") { haptic(); viewModel.startStop() },
                        null,
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

    LightLazyScrollView(
        modifier = modifier,
        scrollBarPosition = LightScrollBarPosition.Inside,
        uniformItemHeightGridUnits = 2.63f,
    ) {
        // the lap in progress, counting — only once a lap has been recorded
        if (isRunning && laps.isNotEmpty()) {
            item(key = "live") {
                LapRow(
                    label = "Lap ${laps.size + 1}",
                    split = elapsed - (laps.lastOrNull() ?: 0L),
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
                label = "Lap ${i + 1}",
                split = splits[i],
                total = laps[i],
            )
        }
    }
}

@Composable
private fun LapRow(
    label: String,
    split: Long,
    total: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // start inset for the grid margin; end inset keeps rows clear of
            // the right-edge scrollbar
            .padding(start = 28.dp, end = 3f.gridUnitsAsDp(), top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightText(
            text = label,
            variant = LightTextVariant.Copy,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        // lap split and running total — same size, each anchored in its own column
        TimeCell(text = compactTime(split))
        Spacer(modifier = Modifier.width(14.dp))
        TimeCell(text = compactTime(total))
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
