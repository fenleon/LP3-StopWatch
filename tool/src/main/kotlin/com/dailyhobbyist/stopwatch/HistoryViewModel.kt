package com.dailyhobbyist.stopwatch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {

    val sessions = MutableStateFlow<List<StopwatchSession>>(emptyList())

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        // Reload every time the screen appears so a newly-saved run (or a
        // deletion from the detail screen) is reflected on return.
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            sessions.value = StopwatchHistory.loadSorted(dataStore)
        }
    }
}
