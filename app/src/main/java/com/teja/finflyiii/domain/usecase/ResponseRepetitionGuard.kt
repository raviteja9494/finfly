/* Pure guard for stopping runaway local-model repetition before it pressures the UI. */
package com.teja.finflyiii.domain.usecase

internal object ResponseRepetitionGuard {
    fun isRunaway(text: String): Boolean {
        val words = text.lowercase().trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.size < MINIMUM_WORDS) return false
        for (windowSize in 1..MAX_PATTERN_WORDS) {
            val repeatedWordCount = windowSize * REQUIRED_REPETITIONS
            if (words.size < repeatedWordCount) continue
            val tail = words.takeLast(repeatedWordCount)
            val pattern = tail.takeLast(windowSize)
            if (tail.chunked(windowSize).all { it == pattern }) return true
        }
        return false
    }

    private const val REQUIRED_REPETITIONS = 4
    private const val MAX_PATTERN_WORDS = 4
    private const val MINIMUM_WORDS = 4
}
