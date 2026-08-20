package network.bahn.androidcryptowallet.ui.bitcoin.setup

object BitcoinPlaceholderMnemonic {
    val WORDS = listOf(
        "witch",
        "banana",
        "cherry",
        "dizzy",
        "echo",
        "filter",
        "gadget",
        "hover",
        "island",
        "jungle",
        "kiwi",
        "lavender",
    )

    val QUIZ_INDICES = listOf(2, 6, 10)

    fun quizQuestions(words: List<String> = WORDS): List<BitcoinMnemonicQuizQuestion> {
        val usable = QUIZ_INDICES.filter { it < words.size }
        return usable.map { index ->
            val correct = words[index]
            val others = words.filterIndexed { i, _ -> i != index }
            val decoys = listOf(
                others[index % others.size],
                others[(index + 4) % others.size],
            )
            BitcoinMnemonicQuizQuestion(
                wordNumber = index + 1,
                correctWord = correct,
                options = (listOf(correct) + decoys).distinct().sorted(),
            )
        }
    }
}

data class BitcoinMnemonicQuizQuestion(
    val wordNumber: Int,
    val correctWord: String,
    val options: List<String>,
)
