package com.example.cardwords.data.model

enum class BonusType { NONE, STREAK_BONUS, PERFECT_SCORE, LEVEL_UP }

data class SessionReward(
    val xpReward: XpReward,
    val motivationalMessage: String,
    val emoji: String,
    val bonusType: BonusType,
)

object RewardGenerator {

    private val perfectMessages = listOf(
        "Безупречно! Ни одной ошибки!",
        "Идеальный результат! Вы мастер!",
        "100%! Невероятная точность!",
        "Совершенство! Так держать!",
    )

    private val goodMessages = listOf(
        "Отличная работа! Продолжайте!",
        "Хороший результат! Вы на верном пути!",
        "Здорово! Ещё немного практики!",
        "Молодец! Прогресс налицо!",
    )

    private val okMessages = listOf(
        "Неплохо! Практика — ключ к успеху!",
        "Продолжайте тренироваться!",
        "С каждой сессией лучше!",
        "Не сдавайтесь — вы растёте!",
    )

    private val levelUpMessages = listOf(
        "Новый уровень! Вы становитесь сильнее!",
        "Уровень повышен! Впечатляющий прогресс!",
        "Поздравляем с новым уровнем!",
    )

    fun generateSessionReward(
        xpReward: XpReward,
        accuracy: Float,
        streak: Int,
    ): SessionReward {
        val bonusType = when {
            xpReward.leveledUp -> BonusType.LEVEL_UP
            accuracy >= 1f && xpReward.perfectBonus > 0 -> BonusType.PERFECT_SCORE
            streak >= 3 && xpReward.streakBonus > 0 -> BonusType.STREAK_BONUS
            else -> BonusType.NONE
        }

        val emoji = when (bonusType) {
            BonusType.LEVEL_UP -> "\uD83C\uDF1F"      // star2
            BonusType.PERFECT_SCORE -> "\uD83D\uDCAF"  // 100
            BonusType.STREAK_BONUS -> "\uD83D\uDD25"   // fire
            BonusType.NONE -> when {
                accuracy >= 0.7f -> "\uD83D\uDC4D"     // thumbs up
                else -> "\uD83D\uDCAA"                   // flexed biceps
            }
        }

        val message = when (bonusType) {
            BonusType.LEVEL_UP -> levelUpMessages.random()
            BonusType.PERFECT_SCORE -> perfectMessages.random()
            else -> when {
                accuracy >= 0.7f -> goodMessages.random()
                else -> okMessages.random()
            }
        }

        return SessionReward(
            xpReward = xpReward,
            motivationalMessage = message,
            emoji = emoji,
            bonusType = bonusType,
        )
    }
}
