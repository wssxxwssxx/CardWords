package com.example.cardwords.data.remote

import com.example.cardwords.data.local.DatabaseRepository

class AuthManager(private val repository: DatabaseRepository) {

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun getToken(): String? = repository.getSetting("api_token")?.takeIf { it.isNotEmpty() }

    fun saveSession(token: String, email: String, name: String, subscription: String) {
        repository.setSetting("api_token", token)
        repository.setSetting("api_user_email", email)
        repository.setSetting("api_user_name", name)
        repository.setSetting("api_subscription", subscription)
    }

    fun getUserEmail(): String? = repository.getSetting("api_user_email")?.takeIf { it.isNotEmpty() }

    fun getUserName(): String? = repository.getSetting("api_user_name")?.takeIf { it.isNotEmpty() }

    fun getSubscription(): String = repository.getSetting("api_subscription")?.takeIf { it.isNotEmpty() } ?: "free"

    fun logout() {
        repository.clearAllUserData()
    }
}
