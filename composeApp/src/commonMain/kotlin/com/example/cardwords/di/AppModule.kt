package com.example.cardwords.di

import com.example.cardwords.data.local.DatabaseRepository
import com.example.cardwords.data.remote.AuthManager
import com.example.cardwords.data.remote.CardWordsApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppModule {
    private var _databaseRepository: DatabaseRepository? = null

    fun initialize(repository: DatabaseRepository) {
        repository.prepopulateIfEmpty()
        _databaseRepository = repository
    }

    val databaseRepository: DatabaseRepository
        get() = _databaseRepository ?: error("AppModule not initialized. Call initialize() first.")

    val cardWordsApiClient: CardWordsApiClient by lazy { CardWordsApiClient() }

    val authManager: AuthManager by lazy { AuthManager(databaseRepository, cardWordsApiClient) }

    /**
     * Application-lifetime scope for fire-and-forget server sync operations
     * (deletions, bulk uploads) that MUST NOT be cancelled when a ViewModel clears.
     * Uses SupervisorJob so one failure doesn't cancel other pending sync tasks.
     */
    val syncScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
