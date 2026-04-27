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

    private val _cardWordsApiClient: CardWordsApiClient by lazy { CardWordsApiClient() }
    private var _cardWordsApiClientOverride: CardWordsApiClient? = null

    val cardWordsApiClient: CardWordsApiClient
        get() = _cardWordsApiClientOverride ?: _cardWordsApiClient

    /**
     * Test-only seam: override the API client used by [authManager] and ViewModels.
     * Must be called BEFORE the first access to [authManager] (which captures
     * [cardWordsApiClient] eagerly via `by lazy`).
     */
    fun setCardWordsApiClientForTesting(client: CardWordsApiClient) {
        _cardWordsApiClientOverride = client
    }

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
