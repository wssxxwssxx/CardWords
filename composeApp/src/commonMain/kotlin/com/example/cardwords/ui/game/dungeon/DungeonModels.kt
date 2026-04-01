package com.example.cardwords.ui.game.dungeon

import com.example.cardwords.data.model.Word

data class DungeonCard(
    val word: Word,
    val power: Int,
    val masteryLevel: Int,
    val upgraded: Boolean = false,
)

data class Monster(
    val name: String,
    val emoji: String,
    val maxHp: Int,
    val attackPower: Int,
    val floor: Int,
)

data class Relic(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val effect: RelicEffect,
)

enum class RelicEffect {
    EXTRA_HEART,
    POWER_BOOST,
    COMBO_MASTER,
    SHIELD,
    LUCKY_DRAW,
    SECOND_CHANCE,
}

sealed class FloorReward {
    abstract val title: String
    abstract val description: String
    abstract val emoji: String

    data class NewCard(val card: DungeonCard) : FloorReward() {
        override val title = card.word.original
        override val description = "\u041D\u043E\u0432\u0430\u044F \u043A\u0430\u0440\u0442\u0430 (\u0441\u0438\u043B\u0430 ${card.power})"
        override val emoji = "\uD83C\uDCCF"
    }

    data class RelicReward(val relic: Relic) : FloorReward() {
        override val title = relic.name
        override val description = relic.description
        override val emoji = relic.emoji
    }

    data class CardUpgrade(val cardIndex: Int, val card: DungeonCard, val powerBoost: Int) : FloorReward() {
        override val title = "\u2B06 ${card.word.original}"
        override val description = "\u0421\u0438\u043B\u0430 +$powerBoost"
        override val emoji = "\u2728"
    }

    data object RemoveCard : FloorReward() {
        override val title = "\u0423\u0434\u0430\u043B\u0438\u0442\u044C \u043A\u0430\u0440\u0442\u0443"
        override val description = "\u0423\u0431\u0435\u0440\u0438\u0442\u0435 \u0441\u043B\u0430\u0431\u0443\u044E \u043A\u0430\u0440\u0442\u0443 \u0438\u0437 \u043A\u043E\u043B\u043E\u0434\u044B"
        override val emoji = "\uD83D\uDDD1"
    }

    data object HealReward : FloorReward() {
        override val title = "\u041B\u0435\u0447\u0435\u043D\u0438\u0435"
        override val description = "+1 HP"
        override val emoji = "\u2764\uFE0F"
    }
}

enum class DungeonPhase {
    NOT_STARTED,
    DECK_PREVIEW,
    COMBAT,
    COMBAT_RESULT,
    FLOOR_CLEARED,
    REWARD_SELECTION,
    REMOVE_CARD_SELECTION,
    GAME_OVER,
    VICTORY,
}

enum class CombatResult { CORRECT, INCORRECT }

data class CombatState(
    val monster: Monster,
    val monsterHp: Int,
    val hand: List<DungeonCard>,
    val activeCardIndex: Int = 0,
    val input: String = "",
    val lastResult: CombatResult? = null,
    val lastDamage: Int = 0,
    val categoryComboCount: Int = 0,
    val lastCategory: String = "",
    val shieldUsed: Boolean = false,
    val secondChanceUsed: Boolean = false,
)

data class WordResult(
    val wordId: Long,
    val correct: Boolean,
)

data class DungeonUiState(
    val phase: DungeonPhase = DungeonPhase.NOT_STARTED,
    val currentFloor: Int = 0,
    val totalFloors: Int = 8,
    val playerHp: Int = 5,
    val maxPlayerHp: Int = 5,
    val deck: List<DungeonCard> = emptyList(),
    val drawPile: List<DungeonCard> = emptyList(),
    val combat: CombatState? = null,
    val rewards: List<FloorReward> = emptyList(),
    val relics: List<Relic> = emptyList(),
    val totalCorrect: Int = 0,
    val totalAttempts: Int = 0,
    val floorsCleared: Int = 0,
    val wordResults: List<WordResult> = emptyList(),
    val bestCombo: Int = 0,
    val currentCombo: Int = 0,
    val highestFloor: Int = 0,
    val totalRuns: Int = 0,
    val isEmpty: Boolean = false,
    val xpEarned: Int = 0,
    val runStartedAt: Long = 0,
)

object DungeonConstants {
    const val MIN_WORDS = 3
    const val MAX_DECK_SIZE = 12
    const val STARTING_HP = 5
    const val TOTAL_FLOORS = 8
    const val HAND_SIZE = 3
    const val HAND_SIZE_LUCKY = 4
    const val MIN_POWER = 3
    const val MAX_POWER = 15
    const val COMBO_THRESHOLD = 3
    const val COMBO_THRESHOLD_MASTER = 2
    const val COMBO_MULTIPLIER = 2
    const val MAX_RELICS = 3
    const val CARD_UPGRADE_BOOST = 3

    val MONSTERS = listOf(
        Monster("\u041A\u043D\u0438\u0436\u043D\u044B\u0439 \u0447\u0435\u0440\u0432\u044C", "\uD83D\uDC1B", 15, 2, 1),
        Monster("\u041F\u044B\u043B\u044C\u043D\u044B\u0439 \u0442\u043E\u043C", "\uD83D\uDCD5", 20, 2, 2),
        Monster("\u0428\u0435\u043F\u0447\u0443\u0449\u0438\u0439 \u0441\u0432\u0438\u0442\u043E\u043A", "\uD83D\uDCDC", 25, 3, 3),
        Monster("\u0422\u0451\u043C\u043D\u044B\u0439 \u043B\u0435\u043A\u0441\u0438\u043A\u043E\u043D", "\uD83D\uDCD6", 30, 3, 4),
        Monster("\u0413\u0440\u0430\u043C\u043C\u0430\u0442\u0438\u0447\u0435\u0441\u043A\u0438\u0439 \u0433\u043E\u043B\u0435\u043C", "\uD83D\uDDFF", 35, 4, 5),
        Monster("\u0424\u043E\u043D\u0435\u0442\u0438\u0447\u0435\u0441\u043A\u0438\u0439 \u0444\u0430\u043D\u0442\u043E\u043C", "\uD83D\uDC7B", 40, 5, 6),
        Monster("\u041B\u0438\u043D\u0433\u0432\u0438\u0441\u0442\u0438\u0447\u0435\u0441\u043A\u0438\u0439 \u043B\u0438\u0447", "\uD83D\uDC80", 45, 5, 7),
        Monster("\u0410\u0440\u0445\u0438\u0434\u0435\u043C\u043E\u043D \u0441\u043B\u043E\u0432\u0430\u0440\u044F", "\uD83D\uDC09", 50, 6, 8),
    )

    val ALL_RELICS = listOf(
        Relic("extra_heart", "\u0414\u043E\u043F. \u0441\u0435\u0440\u0434\u0446\u0435", "\u2764\uFE0F", "+1 \u043C\u0430\u043A\u0441. HP", RelicEffect.EXTRA_HEART),
        Relic("power_boost", "\u0421\u0438\u043B\u0430 \u0441\u043B\u043E\u0432\u0430", "\uD83D\uDCAA", "+2 \u0441\u0438\u043B\u0430 \u0432\u0441\u0435\u0445 \u043A\u0430\u0440\u0442", RelicEffect.POWER_BOOST),
        Relic("combo_master", "\u041C\u0430\u0441\u0442\u0435\u0440 \u043A\u043E\u043C\u0431\u043E", "\uD83C\uDFAF", "\u041A\u043E\u043C\u0431\u043E \u0441 2 \u043A\u0430\u0440\u0442", RelicEffect.COMBO_MASTER),
        Relic("shield", "\u0429\u0438\u0442 \u0437\u043D\u0430\u043D\u0438\u0439", "\uD83D\uDEE1\uFE0F", "\u041F\u0435\u0440\u0432\u0430\u044F \u043E\u0448\u0438\u0431\u043A\u0430 \u0431\u0435\u0437 \u0443\u0440\u043E\u043D\u0430", RelicEffect.SHIELD),
        Relic("lucky_draw", "\u0423\u0434\u0430\u0447\u043D\u044B\u0439 \u043A\u043B\u0430\u0434", "\uD83C\uDF40", "\u0420\u0430\u0437\u0434\u0430\u0447\u0430 4 \u043A\u0430\u0440\u0442\u044B", RelicEffect.LUCKY_DRAW),
        Relic("second_chance", "\u0412\u0442\u043E\u0440\u043E\u0439 \u0448\u0430\u043D\u0441", "\u267B\uFE0F", "\u041F\u043E\u0432\u0442\u043E\u0440 \u043F\u0440\u0438 \u043E\u0448\u0438\u0431\u043A\u0435", RelicEffect.SECOND_CHANCE),
    )

    fun cardPower(wordLength: Int, masteryLevel: Int): Int {
        return (wordLength + masteryLevel * 2).coerceIn(MIN_POWER, MAX_POWER)
    }
}
