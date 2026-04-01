package com.example.cardwords.ui.game.dungeon

import com.example.cardwords.data.model.Word
import com.example.cardwords.util.FuzzyMatcher

object DungeonGameEngine {

    fun initRun(
        words: List<Word>,
        masteryMap: Map<Long, Int>,
        now: Long,
    ): DungeonUiState {
        if (words.size < DungeonConstants.MIN_WORDS) {
            return DungeonUiState(isEmpty = true)
        }

        val cards = words.shuffled()
            .take(DungeonConstants.MAX_DECK_SIZE)
            .map { word ->
                val mastery = masteryMap[word.id] ?: 0
                DungeonCard(
                    word = word,
                    power = DungeonConstants.cardPower(word.original.length, mastery),
                    masteryLevel = mastery,
                )
            }

        return DungeonUiState(
            phase = DungeonPhase.DECK_PREVIEW,
            deck = cards,
            drawPile = cards.shuffled(),
            totalFloors = DungeonConstants.TOTAL_FLOORS,
            playerHp = DungeonConstants.STARTING_HP,
            maxPlayerHp = DungeonConstants.STARTING_HP,
            runStartedAt = now,
        )
    }

    fun startCombat(state: DungeonUiState): DungeonUiState {
        val floor = state.currentFloor
        val monster = DungeonConstants.MONSTERS[floor.coerceIn(0, DungeonConstants.MONSTERS.lastIndex)]

        val handSize = if (state.relics.any { it.effect == RelicEffect.LUCKY_DRAW }) {
            DungeonConstants.HAND_SIZE_LUCKY
        } else {
            DungeonConstants.HAND_SIZE
        }

        var drawPile = state.drawPile.ifEmpty { state.deck.shuffled() }
        val hand = drawPile.take(handSize.coerceAtMost(drawPile.size))
        drawPile = drawPile.drop(hand.size)

        return state.copy(
            phase = DungeonPhase.COMBAT,
            drawPile = drawPile,
            combat = CombatState(
                monster = monster,
                monsterHp = monster.maxHp,
                hand = hand,
                activeCardIndex = 0,
            ),
        )
    }

    fun selectCard(state: DungeonUiState, index: Int): DungeonUiState {
        val combat = state.combat ?: return state
        if (index !in combat.hand.indices) return state
        return state.copy(
            combat = combat.copy(
                activeCardIndex = index,
                lastResult = null,
            ),
        )
    }

    fun updateInput(state: DungeonUiState, input: String): DungeonUiState {
        val combat = state.combat ?: return state
        return state.copy(combat = combat.copy(input = input))
    }

    fun submitAnswer(state: DungeonUiState): DungeonUiState {
        val combat = state.combat ?: return state
        if (combat.hand.isEmpty()) return state
        val card = combat.hand[combat.activeCardIndex]
        val input = combat.input.trim()
        if (input.isEmpty()) return state

        val translations = card.word.translation.split(",", ";", "/").map { it.trim() }
        val isCorrect = translations.any { FuzzyMatcher.isCloseEnough(input, it) }

        return if (isCorrect) {
            handleCorrectAnswer(state, combat, card)
        } else {
            handleIncorrectAnswer(state, combat, card)
        }
    }

    private fun handleCorrectAnswer(
        state: DungeonUiState,
        combat: CombatState,
        card: DungeonCard,
    ): DungeonUiState {
        // Category combo
        val sameCategory = card.word.category.isNotEmpty() && card.word.category == combat.lastCategory
        val comboCount = if (sameCategory) combat.categoryComboCount + 1 else 1
        val comboThreshold = if (state.relics.any { it.effect == RelicEffect.COMBO_MASTER }) {
            DungeonConstants.COMBO_THRESHOLD_MASTER
        } else {
            DungeonConstants.COMBO_THRESHOLD
        }

        val isCombo = comboCount >= comboThreshold
        val powerBoost = if (state.relics.any { it.effect == RelicEffect.POWER_BOOST }) 2 else 0
        val baseDamage = card.power + powerBoost
        val damage = if (isCombo) baseDamage * DungeonConstants.COMBO_MULTIPLIER else baseDamage
        val newComboCount = if (isCombo) 0 else comboCount

        val newMonsterHp = combat.monsterHp - damage
        val newHand = combat.hand.toMutableList().apply { removeAt(combat.activeCardIndex) }
        val newCombo = state.currentCombo + 1

        val wordResult = WordResult(wordId = card.word.id, correct = true)

        val newState = state.copy(
            totalCorrect = state.totalCorrect + 1,
            totalAttempts = state.totalAttempts + 1,
            currentCombo = newCombo,
            bestCombo = maxOf(state.bestCombo, newCombo),
            wordResults = state.wordResults + wordResult,
            combat = combat.copy(
                monsterHp = newMonsterHp,
                hand = newHand,
                activeCardIndex = 0,
                input = "",
                lastResult = CombatResult.CORRECT,
                lastDamage = damage,
                categoryComboCount = newComboCount,
                lastCategory = card.word.category,
                secondChanceUsed = false,
            ),
        )

        // Monster defeated
        if (newMonsterHp <= 0) {
            return newState.copy(
                phase = DungeonPhase.FLOOR_CLEARED,
                floorsCleared = state.floorsCleared + 1,
            )
        }

        // Hand empty — draw more
        if (newHand.isEmpty()) {
            return drawNewHand(newState)
        }

        return newState
    }

    private fun handleIncorrectAnswer(
        state: DungeonUiState,
        combat: CombatState,
        card: DungeonCard,
    ): DungeonUiState {
        // Second chance relic
        val hasSecondChance = state.relics.any { it.effect == RelicEffect.SECOND_CHANCE }
        if (hasSecondChance && !combat.secondChanceUsed) {
            return state.copy(
                combat = combat.copy(
                    input = "",
                    lastResult = CombatResult.INCORRECT,
                    lastDamage = 0,
                    secondChanceUsed = true,
                ),
            )
        }

        // Shield relic
        val hasShield = state.relics.any { it.effect == RelicEffect.SHIELD }
        val shieldBlocks = hasShield && !combat.shieldUsed
        val damage = if (shieldBlocks) 0 else combat.monster.attackPower

        val newHp = state.playerHp - damage
        val wordResult = WordResult(wordId = card.word.id, correct = false)

        // Remove failed card from hand, move on
        val newHand = combat.hand.toMutableList().apply { removeAt(combat.activeCardIndex) }

        val newState = state.copy(
            playerHp = newHp,
            totalAttempts = state.totalAttempts + 1,
            currentCombo = 0,
            wordResults = state.wordResults + wordResult,
            combat = combat.copy(
                hand = newHand,
                activeCardIndex = 0,
                input = "",
                lastResult = CombatResult.INCORRECT,
                lastDamage = damage,
                categoryComboCount = 0,
                lastCategory = "",
                shieldUsed = if (shieldBlocks) true else combat.shieldUsed,
                secondChanceUsed = false,
            ),
        )

        // Player died
        if (newHp <= 0) {
            return newState.copy(
                phase = DungeonPhase.GAME_OVER,
                playerHp = 0,
            )
        }

        // Hand empty — draw more
        if (newHand.isEmpty()) {
            return drawNewHand(newState)
        }

        return newState
    }

    private fun drawNewHand(state: DungeonUiState): DungeonUiState {
        val combat = state.combat ?: return state
        val handSize = if (state.relics.any { it.effect == RelicEffect.LUCKY_DRAW }) {
            DungeonConstants.HAND_SIZE_LUCKY
        } else {
            DungeonConstants.HAND_SIZE
        }

        var drawPile = state.drawPile.ifEmpty { state.deck.shuffled() }
        val hand = drawPile.take(handSize.coerceAtMost(drawPile.size))
        drawPile = drawPile.drop(hand.size)

        return state.copy(
            drawPile = drawPile,
            combat = combat.copy(
                hand = hand,
                activeCardIndex = 0,
            ),
        )
    }

    fun generateRewards(state: DungeonUiState, allWords: List<Word>, masteryMap: Map<Long, Int>): DungeonUiState {
        val deckWordIds = state.deck.map { it.word.id }.toSet()
        val rewards = mutableListOf<FloorReward>()

        // New card (word not in deck)
        val availableWords = allWords.filter { it.id !in deckWordIds }
        if (availableWords.isNotEmpty()) {
            val word = availableWords.random()
            val mastery = masteryMap[word.id] ?: 0
            val card = DungeonCard(
                word = word,
                power = DungeonConstants.cardPower(word.original.length, mastery),
                masteryLevel = mastery,
            )
            rewards.add(FloorReward.NewCard(card))
        }

        // Relic (if < max)
        val ownedRelicIds = state.relics.map { it.id }.toSet()
        val availableRelics = DungeonConstants.ALL_RELICS.filter { it.id !in ownedRelicIds }
        if (availableRelics.isNotEmpty() && state.relics.size < DungeonConstants.MAX_RELICS) {
            rewards.add(FloorReward.RelicReward(availableRelics.random()))
        }

        // Card upgrade
        if (state.deck.isNotEmpty()) {
            val cardToUpgrade = state.deck.indices.random()
            rewards.add(
                FloorReward.CardUpgrade(
                    cardIndex = cardToUpgrade,
                    card = state.deck[cardToUpgrade],
                    powerBoost = DungeonConstants.CARD_UPGRADE_BOOST,
                ),
            )
        }

        // Heal (if below max)
        if (state.playerHp < state.maxPlayerHp) {
            rewards.add(FloorReward.HealReward)
        }

        // Remove card (if deck > 3)
        if (state.deck.size > DungeonConstants.MIN_WORDS) {
            rewards.add(FloorReward.RemoveCard)
        }

        // Pick 3 random from available
        val selected = rewards.shuffled().take(3)

        return state.copy(
            phase = DungeonPhase.REWARD_SELECTION,
            rewards = selected,
        )
    }

    fun selectReward(state: DungeonUiState, rewardIndex: Int): DungeonUiState {
        if (rewardIndex !in state.rewards.indices) return state
        val reward = state.rewards[rewardIndex]

        var newState = when (reward) {
            is FloorReward.NewCard -> state.copy(
                deck = state.deck + reward.card,
            )
            is FloorReward.RelicReward -> {
                var s = state.copy(relics = state.relics + reward.relic)
                if (reward.relic.effect == RelicEffect.EXTRA_HEART) {
                    s = s.copy(
                        maxPlayerHp = s.maxPlayerHp + 1,
                        playerHp = s.playerHp + 1,
                    )
                }
                s
            }
            is FloorReward.CardUpgrade -> {
                val newDeck = state.deck.toMutableList()
                if (reward.cardIndex in newDeck.indices) {
                    val old = newDeck[reward.cardIndex]
                    newDeck[reward.cardIndex] = old.copy(
                        power = (old.power + reward.powerBoost).coerceAtMost(DungeonConstants.MAX_POWER),
                        upgraded = true,
                    )
                }
                state.copy(deck = newDeck)
            }
            is FloorReward.RemoveCard -> {
                return state.copy(phase = DungeonPhase.REMOVE_CARD_SELECTION)
            }
            is FloorReward.HealReward -> state.copy(
                playerHp = (state.playerHp + 1).coerceAtMost(state.maxPlayerHp),
            )
        }

        newState = advanceFloor(newState)
        return newState
    }

    fun removeCardFromDeck(state: DungeonUiState, cardIndex: Int): DungeonUiState {
        if (cardIndex !in state.deck.indices) return state
        if (state.deck.size <= DungeonConstants.MIN_WORDS) return state

        val newDeck = state.deck.toMutableList().apply { removeAt(cardIndex) }
        return advanceFloor(state.copy(deck = newDeck))
    }

    fun skipReward(state: DungeonUiState): DungeonUiState {
        return advanceFloor(state)
    }

    private fun advanceFloor(state: DungeonUiState): DungeonUiState {
        val nextFloor = state.currentFloor + 1
        if (nextFloor >= state.totalFloors) {
            return state.copy(
                phase = DungeonPhase.VICTORY,
                currentFloor = nextFloor,
            )
        }
        return state.copy(
            currentFloor = nextFloor,
            drawPile = state.deck.shuffled(),
            combat = null,
            rewards = emptyList(),
            phase = DungeonPhase.COMBAT,
        ).let { startCombat(it) }
    }
}
