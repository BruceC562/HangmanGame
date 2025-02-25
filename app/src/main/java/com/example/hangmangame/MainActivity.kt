package com.example.hangmangame

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.hangmangame.ui.theme.HangmanGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser

data class HangmanWord (val word: String, val hint: String)

// Loads flashcards from the flashcards.xml
fun loadWords(xmlFile: Int, context: Context): List<HangmanWord> {
    val gameWords = mutableListOf<HangmanWord>()
    val parser = context.resources.getXml(xmlFile)
    var eventType = parser.eventType
    var word = ""
    var hint = ""
    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                when (parser.name) {
                    "phrase" -> {
                        word = parser.nextText()
                    }
                    "hint" -> {
                        hint = parser.nextText()
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.name) {
                    "newGame" -> {
                        gameWords.add(HangmanWord(word, hint))
                    }
                }
            }
        }
        eventType = parser.next()
    }
    return gameWords
}

class MainActivity : ComponentActivity() {
    private val gameWords: List<HangmanWord> by lazy { loadWords(R.xml.game_words, this) } // Loads all possible words for the Hangman game
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HangmanGameTheme {
                val hangmanViewModel: HangmanViewModel = viewModel()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HangmanGame(
                        modifier = Modifier.padding(innerPadding), gameWords, viewModel = hangmanViewModel
                    )
                }
            }
        }
    }
}



@Composable
fun HangmanGame(modifier: Modifier = Modifier, words: List<HangmanWord> = listOf(), viewModel: HangmanViewModel) {
    val currentWord = viewModel.currentWord
    val hint = viewModel.hint
    val hintCounter = viewModel.hintCounter
    val lifeCounter = viewModel.lifeCounter
    val letterOptions = viewModel.letterOptions
    val gameState = viewModel.gameState

    val configuration = LocalConfiguration.current //Gets the current orientation
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val displayLandscape by remember(configuration) { // Recomposes when the orientation changes
        mutableStateOf(
            windowAdaptiveInfo.windowSizeClass // Checks for tablet landscape orientation
                .windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
        )
    }

    val hangmanImage = when (lifeCounter) {
        0 -> R.drawable.hangman_0
        1 -> R.drawable.hangman_1
        2 -> R.drawable.hangman_2
        3 -> R.drawable.hangman_3
        4 -> R.drawable.hangman_4
        5 -> R.drawable.hangman_5
        else -> R.drawable.hangman_6
    }

    // Create a map of functions if needed
    val functionList = mapOf(
        "onLetterClick" to viewModel::onLetterClick,
        "resetGame" to { viewModel.resetGame(words) },
        "hint1" to viewModel::hint1,
        "hint2" to viewModel::hint2,
        "hint3" to viewModel::hint3
    )

    if (displayLandscape) {
        LandscapeHangman(currentWord, lifeCounter, hangmanImage, hint, hintCounter, letterOptions, gameState, functionList)
    } else {
        PortraitHangman(currentWord, lifeCounter, hangmanImage, letterOptions, gameState, functionList)
    }

    if (gameState == GameState.WIN || gameState == GameState.LOSE) {
        GameOverScreen(gameState) {
            viewModel.resetGame(words)
        }
    }
}

@Composable
fun PortraitHangman(
    currentWord: String, lifeCounter: Int, hangmanImage: Int,
    options: List<Char>, gameState: GameState, functionList: Map<String, Any>) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Text(
            text = "Hangman",
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp)
        )
        Button(
            onClick = {
                (functionList["resetGame"] as () -> Unit)() //Gemini used for calling functions from the map
            },
            modifier = Modifier.padding(5.dp)
        ) {
            val isFirstGame = gameState == GameState.STOPPED
            Text(
                text = if (!isFirstGame) "New Game" else "Start",
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
        ) {
            Image(
                painter = painterResource(id = hangmanImage),
                contentDescription = "Hangman $lifeCounter",
                modifier = Modifier.fillMaxSize()
            )
        }
        BuildText(currentWord, 45, modifier = Modifier.weight(1f))
        Text(
            text = "Choose a Letter",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 45.dp, bottom = 5.dp)
        )
        BuildOptions(Modifier.weight(1f), options, false, gameState) { letter ->
            (functionList["onLetterClick"] as (Char) -> Unit)(letter)
        }
    }
}

@Composable
fun LandscapeHangman(
    currentWord: String, lifeCounter: Int, hangmanImage: Int, hint: String, hintCounter: Int,
    options: List<Char>, gameState: GameState, functionList: Map<String, Any>) {
    var displayHint by remember { mutableStateOf("") }
    val toast = Toast.makeText(LocalContext.current, "Hint Unavailable", Toast.LENGTH_SHORT)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Text(
            text = "Hangman",
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp)
        )
        Button(
            onClick = {
                displayHint = ""
                (functionList["resetGame"] as () -> Unit)() //Gemini used for calling functions from the map
            },
            modifier = Modifier.padding(0.dp)
        ) {
            val isFirstGame = gameState == GameState.STOPPED
            Text(
                text = if (!isFirstGame) "New Game" else "Start",
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Choose a Letter",
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                )
                BuildOptions(Modifier.weight(1f), options, true, gameState) { letter ->
                    (functionList["onLetterClick"] as (Char) -> Unit)(letter)
                }
                Box (
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(.5f)
                ) {
                    Button(
                        onClick = {
                            when (hintCounter) {
                                1 -> {
                                    (functionList["hint1"] as () -> Unit)()
                                    displayHint = hint
                                }
                                2 -> if (lifeCounter != 5) (functionList["hint2"] as () -> Unit)() else toast.show()
                                3 -> if (lifeCounter != 5) (functionList["hint3"] as () -> Unit)() else toast.show()
                                else -> toast.show()
                            }
                        },
                        modifier = Modifier.offset(y = (-65).dp)
                    ) {
                        Text(text = "Hint")
                    }
                    Text(
                        text = displayHint,
                        textAlign = TextAlign.Center,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 45.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                ) {
                    Image(
                        painter = painterResource(id = hangmanImage),
                        contentDescription = "Hangman $lifeCounter",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                BuildText(currentWord, 60, modifier = Modifier
                    .weight(1f)
                    .offset(y = (-60).dp))
            }
        }
    }
}

//Builds the display text represented as currentWord
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildText(currentWord: String, textSize: Int, modifier: Modifier = Modifier) {
    val words = currentWord.split(" ") // Split the string into words

    FlowRow(
        modifier = modifier.padding(0.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        //By looping through words, it ensures the whole row can fit on the row its on
        //Assisted by Gemini
        words.forEach { word ->
            Row {
                word.forEach { letter ->
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.padding(horizontal = 3.dp)
                    ) {
                        Text(
                            text = if (letter != '_') letter.toString() else "",
                            fontSize = textSize.sp,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "_",
                            fontSize = (textSize+15).sp,
                            color = Color.Black,
                            modifier = Modifier.offset(y = 2.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

//Builds the letter options and changes the layout based on screen orientation
@Composable
fun BuildOptions(modifier: Modifier = Modifier, options: List<Char>, isLandscape: Boolean, gameState: GameState = GameState.STOPPED, onLetterClick: (Char) -> Unit) {
    if (isLandscape) {
        LazyVerticalGrid (
            columns = GridCells.Fixed(5),
            modifier = modifier.padding(10.dp)
        ){
            items(options) {letter ->
                LetterCard(letter, gameState, onLetterClick = { onLetterClick(letter) })
            }
        }
    } else {
        LazyRow(modifier = modifier.padding(10.dp)) {
            items(options) {letter ->
                LetterCard(letter, gameState, onLetterClick = { onLetterClick(letter) })
            }
        }
    }
}

//Builds each letter for the letter options (called from BuildOptions)
@Composable
fun LetterCard(letter: Char, gameState: GameState, modifier: Modifier = Modifier, onLetterClick: () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(Color.LightGray),
        elevation = CardDefaults.cardElevation(5.dp),
        modifier = modifier
            .padding(5.dp)
            .size(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (gameState == GameState.INPROGRESS) Modifier.clickable { onLetterClick() }
                else Modifier // No clickable modifier when game is not in progress
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(60.dp)
        ) {
            Text(
                text = letter.toString(),
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(5.dp)
            )
        }
    }
}

@Composable
fun GameOverScreen(gameState: GameState, onRestart: () -> Unit) {
    val message = if (gameState == GameState.WIN) "You Win!" else "You Lose!"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(20.dp)
                .background(Color.White, shape = RoundedCornerShape(15.dp))
                .padding(20.dp)
        ) {
            Text(
                text = message,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Button(
                onClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        onRestart()
                    }
                },
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(text = "New Game")
            }
        }
    }
}