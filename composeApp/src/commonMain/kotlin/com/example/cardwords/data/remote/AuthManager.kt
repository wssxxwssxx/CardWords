package com.example.cardwords.data.remote

import com.example.cardwords.data.local.DatabaseRepository
import com.example.cardwords.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(
    private val repository: DatabaseRepository,
    private val apiClient: CardWordsApiClient,
) {
    companion object {
        private const val KEY_TOKEN = "api_token"
        private const val KEY_EMAIL = "api_user_email"
        private const val KEY_NAME = "api_user_name"
        private const val KEY_SUBSCRIPTION = "api_subscription"
        private const val KEY_ROLE = "user_role"
    }

    private val _roleFlow = MutableStateFlow(loadRoleFromSettings())
    val roleFlow: StateFlow<UserRole?> = _roleFlow.asStateFlow()

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()
    fun getToken(): String? = repository.getSetting(KEY_TOKEN)?.takeIf { it.isNotEmpty() }
    fun getUserEmail(): String? = repository.getSetting(KEY_EMAIL)?.takeIf { it.isNotEmpty() }
    fun getUserName(): String? = repository.getSetting(KEY_NAME)?.takeIf { it.isNotEmpty() }
    fun getSubscription(): String =
        repository.getSetting(KEY_SUBSCRIPTION)?.takeIf { it.isNotEmpty() } ?: "free"

    fun getRole(): UserRole? = _roleFlow.value

    fun saveSession(
        token: String,
        email: String,
        name: String,
        subscription: String,
        role: UserRole?,
    ) {
        repository.setSetting(KEY_TOKEN, token)
        repository.setSetting(KEY_EMAIL, email)
        repository.setSetting(KEY_NAME, name)
        repository.setSetting(KEY_SUBSCRIPTION, subscription)
        persistRole(role)
        _roleFlow.value = role
    }

    fun logout() {
        repository.clearAllUserData()
        _roleFlow.value = null
    }

    /**
     * Pushes new role to server, persists locally on success.
     * @return success → updated role; failure → unchanged.
     */
    suspend fun setRole(role: UserRole): Result<Unit> {
        val token = getToken() ?: return Result.failure(IllegalStateException("Not logged in"))
        val result = apiClient.switchRole(token, role.wireValue)
        if (result.isSuccess) {
            persistRole(role)
            _roleFlow.value = role
        }
        return result
    }

    /**
     * Pulls /auth/me, updates roleFlow if the server-side role differs.
     * Use on app foreground / on 403 from role-restricted endpoints.
     */
    suspend fun refreshRoleFromServer(): Result<UserRole?> {
        val token = getToken() ?: return Result.failure(IllegalStateException("Not logged in"))
        return apiClient.getMe(token).map { user ->
            val role = UserRole.fromWire(user.role)
            persistRole(role)
            _roleFlow.value = role
            role
        }
    }

    private fun persistRole(role: UserRole?) {
        repository.setSetting(KEY_ROLE, role?.name ?: "")
    }

    private fun loadRoleFromSettings(): UserRole? {
        val raw = repository.getSetting(KEY_ROLE)?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { UserRole.valueOf(raw) }.getOrNull()
    }
}
