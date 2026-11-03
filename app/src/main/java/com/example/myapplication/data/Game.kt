package com.example.myapplication.data

data class Game(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String?
)

sealed interface UiState {
    data object Loading : UiState
    data class Success(val games: List<Game>) : UiState
    data class Error(val message: String) : UiState
}