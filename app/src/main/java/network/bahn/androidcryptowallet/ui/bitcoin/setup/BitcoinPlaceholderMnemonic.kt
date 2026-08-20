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

    fun quizQuestions(): List<BitcoinMnemonicQuizQuestion> = QUIZ_INDICES.map { index ->
        val correct = WORDS[index]
        val others = WORDS.filterIndexed { i, _ -> i != index }
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

data class BitcoinMnemonicQuizQuestion(
    val wordNumber: Int,
    val correctWord: String,
    val options: List<String>,
)
