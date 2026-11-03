package com.example.myapplication

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Game
import com.example.myapplication.data.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// ---------- Цвета в стиле Minecraft ----------
private val Dirt = Color(0xFF3A2A1C)
private val DirtLight = Color(0xFF59402B)
private val Grass = Color(0xFF5B8731)
private val GrassDark = Color(0xFF3F6B1F)
private val Stone = Color(0xFF7F7F7F)
private val PanelColor = Color(0xFF2B2B2B)
private val Gold = Color(0xFFFFD83D)
private val Redstone = Color(0xFFD13B2E)

class MainActivity : ComponentActivity() {

    private val vm: GamesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Grass,
                    onPrimary = Color.White,
                    background = Dirt,
                    surface = PanelColor
                )
            ) {
                // Моноширинный шрифт для всего текста — «пиксельный» вид
                ProvideTextStyle(TextStyle(fontFamily = FontFamily.Monospace, color = Color.White)) {
                    MainScreen(vm)
                }
            }
        }
    }
}

@Composable
fun MainScreen(vm: GamesViewModel) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Surface(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        color = Dirt
    ) {
        Column(Modifier.fillMaxSize()) {
            Header()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val state = vm.state) {
                    is UiState.Loading -> LoadingView()
                    is UiState.Error -> ErrorView(state.message, onRetry = vm::load)
                    is UiState.Success -> {
                        val games = state.games
                        val index = vm.index.coerceIn(0, games.lastIndex)

                        val details: @Composable () -> Unit = {
                            Details(
                                games = games,
                                index = index,
                                showFull = vm.showFullDescription,
                                onShowFullChange = { vm.showFullDescription = it },
                                onPrevious = vm::previous,
                                onNext = vm::next,
                                onSelect = vm::select,
                                onRefresh = vm::load
                            )
                        }

                        if (landscape) {
                            Row(
                                Modifier.fillMaxSize().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                GameImage(games[index], Modifier.weight(1f).fillMaxHeight())
                                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { details() }
                            }
                        } else {
                            Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
                            ) {
                                GameImage(games[index], Modifier.fillMaxWidth().height(260.dp))
                                Spacer(Modifier.height(12.dp))
                                details()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Шапка: трава + название ----------

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(10.dp).background(Grass))
        Box(
            Modifier.fillMaxWidth().background(DirtLight).padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "MINECRAFT · КАТАЛОГ",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color.Black))
    }
}

// ---------- Блочные элементы ----------

/** Кнопка как каменный блок с «объёмной» рамкой. */
@Composable
private fun McButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Stone
) {
    Box(
        modifier
            .clickable(onClick = onClick)
            .background(Color.Black)
            .padding(2.dp)
            .background(color)
            .drawBehind {
                val t = 3.dp.toPx()
                val light = Color(0x55FFFFFF)
                val shade = Color(0x66000000)
                drawRect(light, Offset(0f, 0f), Size(size.width, t))
                drawRect(light, Offset(0f, 0f), Size(t, size.height))
                drawRect(shade, Offset(0f, size.height - t), Size(size.width, t))
                drawRect(shade, Offset(size.width - t, 0f), Size(t, size.height))
            }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = Color.White
        )
    }
}

/** Тёмная панель с чёрной рамкой. */
@Composable
private fun McPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .border(3.dp, Color.Black)
            .background(PanelColor)
            .padding(12.dp)
    ) { content() }
}

// ---------- Картинка (на всю рамку) ----------

@Composable
private fun GameImage(game: Game, modifier: Modifier = Modifier) {
    Box(
        modifier
            .border(4.dp, Color.Black)
            .background(Color(0xFF111111)),
        contentAlignment = Alignment.Center
    ) {
        if (game.imageUrl != null) {
            RemoteImage(url = game.imageUrl, description = game.title, modifier = Modifier.fillMaxSize())
        } else {
            Text("Нет изображения")
        }
    }
}

/** Загрузка картинки по URL без сторонних библиотек (в фоновом потоке). */
@Composable
private fun RemoteImage(url: String, description: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        try {
            val loaded = withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 8_000
                    connection.readTimeout = 8_000
                    connection.setRequestProperty("User-Agent", "MyApplication/1.0 (student lab work)")
                    connection.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                } finally {
                    connection.disconnect()
                }
            }
            if (loaded == null) failed = true else bitmap = loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }

    val image = bitmap
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = description,
                contentScale = ContentScale.Crop,   // картинка на всю рамку
                modifier = Modifier.fillMaxSize()
            )
            failed -> Text("Не удалось загрузить картинку")
            else -> CircularProgressIndicator(color = Grass)
        }
    }
}

// ---------- Секции экрана ----------

@Composable
private fun Details(
    games: List<Game>,
    index: Int,
    showFull: Boolean,
    onShowFullChange: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GameInfo(games[index], showFull)
        FullDescriptionSwitch(showFull, onShowFullChange)
        ControlPanel(index, games.size, onPrevious, onNext)
        McButton("ОБНОВИТЬ", onRefresh, Modifier.fillMaxWidth(), color = GrassDark)
        GamesList(games, index, onSelect)
    }
}

@Composable
private fun GameInfo(game: Game, showFull: Boolean) {
    McPanel {
        Text(
            game.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Gold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            game.description.ifBlank { "Описание отсутствует." },
            fontSize = 14.sp,
            color = Color(0xFFE0E0E0),
            maxLines = if (showFull) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FullDescriptionSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    McPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Полное описание", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Grass,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Stone
                )
            )
        }
    }
}

@Composable
private fun ControlPanel(index: Int, total: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        McButton("Предыдущая", onPrevious, Modifier.weight(1f))
        Text("${index + 1}/$total", fontWeight = FontWeight.Bold, color = Gold)
        McButton("Следующая", onNext, Modifier.weight(1f))
    }
}

@Composable
private fun GamesList(games: List<Game>, selected: Int, onSelect: (Int) -> Unit) {
    McPanel {
        Text("Загружено записей: ${games.size}", fontWeight = FontWeight.Bold, color = Gold)
        Spacer(Modifier.height(6.dp))
        games.forEachIndexed { i, game ->
            Text(
                "${game.id}. ${game.title}",
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (i == selected) GrassDark else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(8.dp)
            )
        }
    }
}

// ---------- Загрузка и ошибка ----------

@Composable
private fun LoadingView() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Grass)
        Spacer(Modifier.height(12.dp))
        Text("Загрузка…", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        McPanel {
            Text(
                "Не удалось загрузить данные",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Redstone,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(message, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            McButton("ПОВТОРИТЬ", onRetry, Modifier.fillMaxWidth(), color = GrassDark)
        }
    }
}