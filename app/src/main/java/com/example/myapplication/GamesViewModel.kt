package com.example.myapplication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ApiException
import com.example.myapplication.data.GamesRepository
import com.example.myapplication.data.UiState
import kotlinx.coroutines.launch

class GamesViewModel : ViewModel() {

    var state by mutableStateOf<UiState>(UiState.Loading)
        private set
    var index by mutableStateOf(0)
        private set
    var showFullDescription by mutableStateOf(false)

    init { load() }

    fun load() {
        viewModelScope.launch {
            state = UiState.Loading
            state = try {
                val games = GamesRepository.loadGames()
                if (games.isEmpty()) UiState.Error("Сервер вернул пустой ответ.")
                else UiState.Success(games)
            } catch (e: ApiException) {
                UiState.Error(e.message ?: "Неизвестная ошибка")
            }
            index = 0
        }
    }

    private val size get() = (state as? UiState.Success)?.games?.size ?: 0

    fun next() { if (size > 0) index = (index + 1) % size }
    fun previous() { if (size > 0) index = (index - 1 + size) % size }
    fun select(i: Int) { if (i in 0 until size) index = i }
}