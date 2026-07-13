package com.dailyhobbyist.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter

class HistoryScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, HistoryViewModel>(sealedActivity) {

    override val viewModelClass: Class<HistoryViewModel>
        get() = HistoryViewModel::class.java

    override fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(lightContext.dataStore)
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val sessions by viewModel.sessions.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.Text("BACK") { goBack() },
                    center = LightTopBarCenter.Text("HISTORY"),
                )

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "No saved runs yet.",
                            variant = LightTextVariant.Detail,
                            align = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                    ) {
                        items(items = sessions, key = { it.id }) { session ->
                            SessionRow(
                                session = session,
                                onClick = {
                                    navigateTo(screenFactory = { sealed ->
                                        SessionDetailScreen(sealed, session)
                                    })
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: StopwatchSession,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LightText(
                text = formatSessionDate(session.startedAtWall),
                variant = LightTextVariant.Detail,
            )
            LightText(
                text = formatSessionTime(session.startedAtWall) +
                    "  ·  " + lapCountLabel(session.laps.size),
                variant = LightTextVariant.Fine,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        LightText(
            text = formatTime(session.totalMs),
            variant = LightTextVariant.Detail,
        )
    }
}
