package com.example.myapplication.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class ApiException(message: String) : Exception(message)

object GamesRepository {

    private const val LANG = "ru"
    private const val BASE_URL = "https://$LANG.wikipedia.org/api/rest_v1/page/summary/"

    val TITLES = listOf(
        "Minecraft",
        "Minecraft_Dungeons",
        "Minecraft_Legends",
        "Minecraft_Earth",
        "Minecraft:_Story_Mode",
        "Minecraft:_Education_Edition",
        "Mojang_Studios",
        "Xbox_Game_Studios",
        "Minecraft_Classic",
        "Minecraft_Realms"
    )

    suspend fun loadGames(): List<Game> = coroutineScope {
        TITLES.map { title -> async(Dispatchers.IO) { fetchGame(title) } }
            .awaitAll()
            .filterNotNull()
    }

    private fun fetchGame(title: String): Game? {
        val connection = URL(BASE_URL + Uri.encode(title)).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MyApplication/1.0 (student lab work)")

            val code = connection.responseCode
            return when {
                code == HttpURLConnection.HTTP_OK -> {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    parse(text)
                }
                code == HttpURLConnection.HTTP_NOT_FOUND -> null
                code in 500..599 -> throw ApiException("Ошибка сервера (HTTP $code). Попробуйте позже.")
                else -> throw ApiException("Неожиданный ответ сервера (HTTP $code).")
            }
        } catch (e: UnknownHostException) {
            throw ApiException("Нет подключения к интернету.")
        } catch (e: SocketTimeoutException) {
            throw ApiException("Сервер не отвечает (превышено время ожидания).")
        } catch (e: IOException) {
            throw ApiException("Ошибка сети: ${e.message ?: "неизвестная"}")
        } catch (e: JSONException) {
            throw ApiException("Не удалось разобрать ответ сервера.")
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: String): Game {
        val obj = JSONObject(json)
        return Game(
            id = obj.optInt("pageid"),
            title = obj.optString("title"),
            description = obj.optString("extract"),
            imageUrl = obj.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
        )
    }
}