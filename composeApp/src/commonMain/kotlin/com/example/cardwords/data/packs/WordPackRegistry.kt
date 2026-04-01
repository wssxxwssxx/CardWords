package com.example.cardwords.data.packs

import com.example.cardwords.data.model.PackLevel
import com.example.cardwords.data.model.WordPack

object WordPackRegistry {
    val allPacks: List<WordPack> = listOf(
        BasicA1Pack,
        PopularB1Pack,
        AdvancedB2Pack,
        ITTechPack,
        TravelPack,
        BusinessPack,
        EverydayLifePack,
        PhrasesIdiomsPack,
    )

    val byLevel: List<WordPack> get() = allPacks.filter { it.level == PackLevel.BY_LEVEL }
    val byTopic: List<WordPack> get() = allPacks.filter { it.level == PackLevel.BY_TOPIC }
    val special: List<WordPack> get() = allPacks.filter { it.level == PackLevel.SPECIAL }

    fun findById(id: String): WordPack? = allPacks.find { it.id == id }
}
