package com.example.cardwords.data.repository

import com.example.cardwords.data.model.Word

interface WordRepository {
    fun getAllWords(): List<Word>
    fun getWordById(id: Long): Word?
}

class HardcodedWordRepository : WordRepository {

    private val words = listOf(
        Word(1, "Hello", "Привет", "[hɛˈloʊ]", "Приветствия"),
        Word(2, "Goodbye", "До свидания", "[ɡʊdˈbaɪ]", "Приветствия"),
        Word(3, "Thank you", "Спасибо", "[θæŋk juː]", "Вежливость"),
        Word(4, "Please", "Пожалуйста", "[pliːz]", "Вежливость"),
        Word(5, "Yes", "Да", "[jɛs]", "Основы"),
        Word(6, "No", "Нет", "[noʊ]", "Основы"),
        Word(7, "Water", "Вода", "[ˈwɔːtər]", "Еда и напитки"),
        Word(8, "Food", "Еда", "[fuːd]", "Еда и напитки"),
        Word(9, "Friend", "Друг", "[frɛnd]", "Люди"),
        Word(10, "Love", "Любовь", "[lʌv]", "Чувства"),
        Word(11, "Time", "Время", "[taɪm]", "Основы"),
        Word(12, "Day", "День", "[deɪ]", "Время"),
        Word(13, "Night", "Ночь", "[naɪt]", "Время"),
        Word(14, "Book", "Книга", "[bʊk]", "Предметы"),
        Word(15, "House", "Дом", "[haʊs]", "Места"),
        Word(16, "Cat", "Кошка", "[kæt]", "Животные"),
        Word(17, "Dog", "Собака", "[dɔːɡ]", "Животные"),
        Word(18, "Sun", "Солнце", "[sʌn]", "Природа"),
        Word(19, "Moon", "Луна", "[muːn]", "Природа"),
        Word(20, "Star", "Звезда", "[stɑːr]", "Природа"),
    )

    override fun getAllWords(): List<Word> = words

    override fun getWordById(id: Long): Word? = words.find { it.id == id }
}
