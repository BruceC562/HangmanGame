package com.example.hangmangame

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.hangmangame.ui.theme.HangmanGameTheme
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HangmanGame(
                        modifier = Modifier.padding(innerPadding), gameWords
                    )
                }
            }
        }
    }
}



@Composable
fun HangmanGame(modifier: Modifier = Modifier, words: List<HangmanWord> = listOf()) {
    var word by remember { mutableStateOf("") }
    var currentWord by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var hintCounter by remember { mutableIntStateOf(1) }
    var lifeCounter by remember { mutableIntStateOf(0) }
    val letterOptions = remember { mutableStateListOf<Char>() }
    var gameState by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current //Gets the current orientation
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val displayLandscape by remember(configuration) { // Recomposes when the orientation changes
        mutableStateOf(
            windowAdaptiveInfo.windowSizeClass // Checks for tablet landscape orientation
                .windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
        )
    }

    fun resetGame() {
        val newWord = words.random()
        word = newWord.word
        //Changing word into underscores written by Gemini
        currentWord = word.map { if (it == ' ') ' ' else '_' }.joinToString("")
        hint = newWord.hint
        hintCounter = 1
        lifeCounter = 0
        gameState = true
        letterOptions.clear()
        letterOptions.addAll(('A'..'Z').toList())
    }

    fun onLetterClick(letter: Char) {
        val newWord = letterCheck(letter, word, currentWord)
        if (newWord == currentWord) {
            lifeCounter++
        } else {
            currentWord = newWord
        }
        letterOptions.remove(letter)
    }

    fun hint2() {
        hintCounter++
        lifeCounter++
        removeHalfAvailableLetters(letterOptions, word)
    }

    fun hint3() {
        hintCounter++
        lifeCounter++
        currentWord = revealVowels(word, currentWord)
    }

    //List of functions to update game variables
    val functionList = mapOf<String, Any>(
        "onLetterClick" to ::onLetterClick,
        "resetGame" to ::resetGame,
        "hint2" to ::hint2,
        "hint3" to ::hint3
    )

    if (displayLandscape) {
        println("reached")
        LandscapeHangman(lifeCounter, hint, hintCounter, letterOptions, gameState, functionList)
    } else {
        PortraitHangman(currentWord, lifeCounter, letterOptions, gameState, functionList)
    }
}

@Composable
fun PortraitHangman(currentWord: String, lifeCounter: Int, options: List<Char>, gameState: Boolean, functionList: Map<String, Any>) {
    val hangmanImage = when (lifeCounter) {
        0 -> R.drawable.hangman_0
        1 -> R.drawable.hangman_1
        2 -> R.drawable.hangman_2
        3 -> R.drawable.hangman_3
        4 -> R.drawable.hangman_4
        5 -> R.drawable.hangman_5
        6 -> R.drawable.hangman_6
        else -> R.drawable.hangman_6 //Lose image
    }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(5.dp)
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
            Text(
                text = if (gameState) "New Game" else "Start",
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f) // Adjust this value to control how much of the screen the image takes
        ) {
            Image(
                painter = painterResource(id = hangmanImage),
                contentDescription = "Hangman $lifeCounter",
                modifier = Modifier.fillMaxSize()
            )
        }
        BuildText(currentWord, modifier = Modifier.weight(1f))
        Text(
            text = "Choose a Letter",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 45.dp, bottom = 5.dp)
        )
        BuildOptions(Modifier.weight(1f), options, false) { letter ->
            (functionList["onLetterClick"] as (Char) -> Unit)(letter)
        }
    }

}

@Composable
fun LandscapeHangman(lifeCounter: Int, hint: String, hintCounter: Int,
                     options: List<Char>, gameState: Boolean, functionList: Map<String, Any>) {
    Row(

    ) {
        Column () {

        }
        Column () {

        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildText(currentWord: String, modifier: Modifier = Modifier) {
    val words = currentWord.split(" ") // Split the string into words

    FlowRow(
        modifier = modifier.padding(0.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        //By looping through words, it ensures the whole row can fit on the row its on
        words.forEach { word ->
            Row {
                word.forEach { letter ->
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.padding(horizontal = 3.dp)
                    ) {
                        Text(
                            text = if (letter != '_') letter.toString() else "",
                            fontSize = 40.sp,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "_",
                            fontSize = 55.sp,
                            color = Color.Black,
                            modifier = Modifier.offset(y = 3.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildOptions(modifier: Modifier = Modifier, options: List<Char>, isLandscape: Boolean, onLetterClick: (Char) -> Unit) {
    if (isLandscape) {
        LazyVerticalGrid (
            columns = GridCells.Fixed(4),
            modifier = modifier
        ){
            items(options) {letter ->
                LetterCard(letter)
            }
        }
    } else {
        LazyRow(modifier = modifier.padding(10.dp)) {
            items(options) {letter ->
                LetterCard(letter, onLetterClick = { onLetterClick(letter) })
            }
        }
    }
}

@Composable
fun LetterCard(letter: Char, modifier: Modifier = Modifier, onLetterClick: () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(Color.LightGray),
        elevation = CardDefaults.cardElevation(5.dp),
        modifier = modifier
            .padding(5.dp)
            .size(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onLetterClick() }
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

//Builds the new currentWord by checking if the letter is in the word
fun letterCheck(letter: Char, word: String, currentWord: String): String {
    var newWord = ""
    for (i in word.indices) {
        newWord +=
            if (word[i] == ' ') ' '
            else {
                if (word[i].uppercaseChar() == letter) word[i]
                else currentWord[i]
            }

    }
    return newWord
}

//Finds available letters not in word and removes half of them
fun removeHalfAvailableLetters(options: MutableList<Char>, word: String) {
    val lettersNotInWord = options.filter { it !in word}.toMutableList()
    lettersNotInWord.shuffle()
    options.removeAll(lettersNotInWord.take(lettersNotInWord.size / 2))
}

//Reveals all vowels in the word
fun revealVowels(word: String, currentWord: String): String {
    val vowels = "AEIOU"
    var newWord = ""
    for (i in word.indices) {
        newWord +=
            if (word[i] == ' ') ' '
            else {
                if (word[i].uppercaseChar() in vowels) word[i]
                else currentWord[i]
            }
    }
    return newWord
}

@Preview(showBackground = true)
@Composable
fun PreviewHangman() {
    HangmanGameTheme {
        HangmanGame(words = listOf(HangmanWord("Testing", "This is a hint")))
    }
}