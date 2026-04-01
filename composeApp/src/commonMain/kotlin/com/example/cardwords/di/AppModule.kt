package com.example.cardwords.di

import com.example.cardwords.data.local.DatabaseRepository
import com.example.cardwords.data.remote.WordApiClient

object AppModule {
    private var _databaseRepository: DatabaseRepository? = null

    fun initialize(repository: DatabaseRepository) {
        repository.prepopulateIfEmpty()
        _databaseRepository = repository
    }

    val databaseRepository: DatabaseRepository
        get() = _databaseRepository ?: error("AppModule not initialized. Call initialize() first.")

    val wordApiClient: WordApiClient by lazy { WordApiClient() }
}
