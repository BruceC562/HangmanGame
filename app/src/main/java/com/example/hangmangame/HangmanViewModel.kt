package com.example.hangmangame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

enum class GameState {
    STOPPED,
    INPROGRESS,
    LOSE,
    WIN
}

class HangmanViewModel : ViewModel() {
    var word by mutableStateOf("")
        private set
    var currentWord by mutableStateOf("")
        private set
    var hint by mutableStateOf("")
        private set
    var hintCounter by mutableIntStateOf(1)
        private set
    var lifeCounter by mutableIntStateOf(0)
        private set
    val letterOptions = mutableStateListOf<Char>()

    var gameState by mutableStateOf(GameState.STOPPED)
        private set

    //Resets the game with a random word
    fun resetGame(words: List<HangmanWord>) {
        val newWord = words.random()
        word = newWord.word
        //Changing word into underscores written by Gemini
        currentWord = word.map { if (it == ' ') ' ' else '_' }.joinToString("")
        hint = newWord.hint
        hintCounter = 1
        lifeCounter = 0
        gameState = GameState.INPROGRESS
        letterOptions.clear()
        letterOptions.addAll(('A'..'Z').toList())
    }

    //Process a letter click
    fun onLetterClick(letter: Char) {
        val newWord = letterCheck(letter, word, currentWord)
        if (newWord == currentWord) {
            // Letter not found
            lifeCounter++
        } else {
            currentWord = newWord
        }
        letterOptions.remove(letter)
        if (lifeCounter == 6) {
            gameState = GameState.LOSE
        } else if (word == currentWord) {
            gameState = GameState.WIN
        }
    }

    fun hint1() {
        hintCounter++
    }

    fun hint2() {
        hintCounter++
        lifeCounter++
        removeHalfAvailableLetters(letterOptions, word)
    }

    fun hint3() {
        hintCounter++
        lifeCounter++
        currentWord = revealVowels(word, currentWord, letterOptions)
    }

    //Builds the new currentWord by checking if the letter is in the word
    private fun letterCheck(letter: Char, word: String, currentWord: String): String {
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
    private fun removeHalfAvailableLetters(options: MutableList<Char>, word: String) {
        val lettersNotInWord = options.filter { it !in word.uppercase() }.toMutableList()
        lettersNotInWord.shuffle()
        options.removeAll(lettersNotInWord.take(lettersNotInWord.size / 2))
    }

    //Reveals all vowels in the word and removes all vowels from options
    private fun revealVowels(word: String, currentWord: String, options: MutableList<Char>): String {
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
        for (char in vowels.toList()) {
            if (char in options)
                options.remove(char)
        }
        return newWord
    }
}
