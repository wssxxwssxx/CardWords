package com.example.cardwords.auth

import com.example.cardwords.data.local.InMemoryDatabaseRepository
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.data.remote.AuthManager
import com.example.cardwords.data.remote.CardWordsApiClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthManagerRoleTest {

    @Test
    fun setRole_happyPath_persistsRoleAndUpdatesFlow() = runBlocking {
        val repo = InMemoryDatabaseRepository()
        // Pre-seed token so AuthManager considers the user logged in.
        repo.setSetting("api_token", "test-token")

        val fakeApi = object : CardWordsApiClient() {
            override suspend fun switchRole(token: String, role: String): Result<Unit> {
                // Sanity-check that AuthManager passes the wire value (lowercase) and the token.
                assertEquals("test-token", token)
                assertEquals(UserRole.TEACHER.wireValue, role)
                return Result.success(Unit)
            }
        }
        val auth = AuthManager(repo, fakeApi)

        assertNull(auth.getRole())
        val result = auth.setRole(UserRole.TEACHER)
        assertTrue(result.isSuccess)
        assertEquals(UserRole.TEACHER, auth.getRole())
        // AuthManager persists the enum name (not the wire value) under "user_role".
        assertEquals("TEACHER", repo.getSetting("user_role"))
    }

    @Test
    fun setRole_withoutToken_failsAndDoesNotPersist() = runBlocking {
        val repo = InMemoryDatabaseRepository()
        // No token seeded — AuthManager should refuse to set a role.

        val fakeApi = object : CardWordsApiClient() {
            override suspend fun switchRole(token: String, role: String): Result<Unit> {
                error("switchRole must NOT be called when there is no token")
            }
        }
        val auth = AuthManager(repo, fakeApi)

        val result = auth.setRole(UserRole.STUDENT)
        assertTrue(result.isFailure)
        assertNull(auth.getRole())
        assertNull(repo.getSetting("user_role"))
    }

    @Test
    fun setRole_apiFailure_doesNotPersistRole() = runBlocking {
        val repo = InMemoryDatabaseRepository()
        repo.setSetting("api_token", "test-token")

        val fakeApi = object : CardWordsApiClient() {
            override suspend fun switchRole(token: String, role: String): Result<Unit> =
                Result.failure(RuntimeException("server down"))
        }
        val auth = AuthManager(repo, fakeApi)

        val result = auth.setRole(UserRole.TEACHER)
        assertTrue(result.isFailure)
        assertNull(auth.getRole())
        assertNull(repo.getSetting("user_role"))
    }
}
