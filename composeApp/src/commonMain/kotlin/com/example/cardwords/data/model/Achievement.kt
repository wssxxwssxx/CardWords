package com.example.cardwords.data.model

data class Achievement(
    val id: Long,
    val type: AchievementType,
    val unlockedAt: Long,
)

enum class AchievementType(
    val title: String,
    val description: String,
    val emoji: String,
) {
    FIRST_STEPS("Первые шаги", "Завершите первую сессию", "\uD83D\uDC63"),
    EXPERT("Знаток", "Освойте 10 слов в любом режиме", "\uD83E\uDDD0"),
    POLYGLOT("Полиглот", "Освойте 50 слов во всех 4 режимах", "\uD83C\uDF0D"),
    ON_FIRE("На огне", "Серия из 7 дней подряд", "\uD83D\uDD25"),
    PERFECTIONIST("Перфекционист", "100% точность (мин. 5 вопросов)", "\uD83C\uDFC6"),
    MARATHON("Марафонец", "Серия из 30 дней подряд", "\uD83C\uDFC3"),
    COLLECTOR("Собиратель", "Добавьте 100 слов в словарь", "\uD83D\uDCDA"),
    BEGINNER("Начинающий", "Завершите 10 сессий", "\uD83C\uDF1F"),
    ERUDITE("Эрудит", "Учите слова из 3+ источников", "\uD83C\uDF93"),
    WORD_MASTER("Мастер слова", "Освойте слово во всех 4 режимах", "\uD83D\uDC8E"),
    GOAL_SETTER("Целеустремлённый", "Выполняйте дневную цель 7 дней подряд", "\uD83C\uDFAF"),
    XP_HUNTER("Охотник за XP", "Наберите 1000 XP", "\u26A1"),
    LEVEL_5("Уровень 5", "Достигните 5-го уровня", "\uD83C\uDFC5"),
    STREAK_GUARDIAN("Хранитель серии", "Используйте заморозку серии", "\u2744\uFE0F"),
    COMEBACK("Камбэк", "Вернитесь после 3+ дней отсутствия и проведите сессию", "\uD83D\uDD04"),
}
