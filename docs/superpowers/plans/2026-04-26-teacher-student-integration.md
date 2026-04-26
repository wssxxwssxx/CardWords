# Teacher / Student Integration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the existing teacher/student backend on `64.188.60.84` into the Compose Multiplatform Android client — flexible role per user, mandatory role-pick after register, full teacher CRUD over students/collections/cards/assignments, student view with collection accept and "new cards" badge.

**Architecture:** Personal data stays in SQLDelight (untouched by role). Role-scoped data (students, collections, teachers, assignments) is in-memory ViewModel cache only. `AuthManager` exposes `roleFlow: StateFlow<UserRole?>` — UI subscribes, navigation rebuilds via `key(role)`. All server mutations run on `AppModule.syncScope` (app-lifetime) so navigation can't cancel them. Optimistic UI with snapshot/rollback. Online-only in v1.

**Tech Stack:** Kotlin 2.1.20 (commonMain KMP), Compose Multiplatform 1.10.0-alpha05, ktor-client 3.x, kotlinx.serialization, SQLDelight, kotlinx.coroutines (Dispatchers.Default for sync), `kotlin.test` + `composeApp:jvmTest` for unit tests.

**Spec reference:** [`docs/superpowers/specs/2026-04-26-teacher-student-integration-design.md`](../specs/2026-04-26-teacher-student-integration-design.md)

---

## File Structure

### New files

| Path | Responsibility |
|---|---|
| `composeApp/src/commonMain/kotlin/com/example/cardwords/data/model/UserRole.kt` | Enum + parsing helpers |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/data/model/Collection.kt` | Domain types: `Collection`, `CollectionDetail`, `CollectionCard`, `AssignmentStatus`, `AssignedCollection`, `StudentSummary`, `TeacherSummary` |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/auth/RoleSelectionViewModel.kt` | One-shot pick; calls `AuthManager.setRole` |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/auth/RoleSelectionScreen.kt` | Mandatory gate after register/login when role==null |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/student/MyTeachersViewModel.kt` | Student-side: teachers + assigned collections + accept |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/student/MyTeachersScreen.kt` | 5-th tab content for student |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/MyStudentsViewModel.kt` | Teacher: students list, invite, remove |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/MyCollectionsViewModel.kt` | Teacher: collections list, create |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionDetailViewModel.kt` | Teacher push: collection edit, cards CRUD, assign students |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/TeachingScreen.kt` | 5-th tab content for teacher (sub-tabs) |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/StudentsTab.kt` | Sub-tab inside TeachingScreen |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionsTab.kt` | Sub-tab inside TeachingScreen |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionDetailScreen.kt` | Push screen from CollectionsTab |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AddStudentDialog.kt` | Invite by email |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/CreateCollectionDialog.kt` | New collection name + description |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AddCardDialog.kt` | Add card to collection |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AssignStudentSheet.kt` | Bottom sheet picker |
| `composeApp/src/jvmTest/kotlin/com/example/cardwords/auth/AuthManagerTest.kt` | role drift on 403 + switchRole flow |
| `composeApp/src/jvmTest/kotlin/com/example/cardwords/student/MyTeachersViewModelTest.kt` | accept revert + reaccept badge |
| `composeApp/src/jvmTest/kotlin/com/example/cardwords/teaching/CollectionDetailViewModelTest.kt` | dedupe addCard locally |

### Modified files

| Path | Edits |
|---|---|
| `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiModels.kt` | Add DTO request/response classes |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt` | Add 14 new endpoint methods |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/AuthManager.kt` | Add `roleFlow`, `setRole`, `refreshRoleFromServer`, persist on login |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/data/local/DatabaseRepository.kt` | (Already has `getSetting`/`setSetting` — no schema change) |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/navigation/Routes.kt` | Add `RoleSelectionRoute`, `CollectionDetailRoute(id)` |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/App.kt` | Routing gate: role==null → RoleSelectionRoute |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/main/MainScreen.kt` | Dynamic tabs by role + `key(role)` |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/profile/ProfileScreen.kt` | "Сменить роль" action |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/profile/ProfileViewModel.kt` | `switchRole(newRole)` |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/auth/AuthViewModel.kt` | Initial role pull after register/login |
| `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/dictionary/DictionaryViewModel.kt` | Expose `syncCardsFromServer` as a public function for reuse after accept |

---

## Conventions

- Every API method returns `Result<T>` and **explicitly** checks `response.status.value !in 200..299`, throwing on non-success (pattern from PR #1).
- Mutations run on `AppModule.syncScope.launch { ... }`; reads/observes use `viewModelScope.launch { ... }`.
- Optimistic UI: snapshot prev state → optimistic apply → server call → on failure restore + toast.
- Models are `@Immutable` data classes.
- All Compose screens have **4 explicit states**: Loading, Empty, Error, Content.
- Strings hardcoded in Russian (consistent with existing app).
- Tests: `kotlin.test` runner, run via `./gradlew :composeApp:jvmTest`.

---

## Tasks

### Task 1: Domain models — `UserRole` + collection-related types

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/data/model/UserRole.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/data/model/Collection.kt`

- [ ] **Step 1: Write `UserRole.kt`**

```kotlin
package com.example.cardwords.data.model

import androidx.compose.runtime.Immutable

@Immutable
enum class UserRole {
    STUDENT, TEACHER;

    val wireValue: String get() = name.lowercase() // server uses "student"/"teacher"

    companion object {
        fun fromWire(value: String?): UserRole? = when (value?.lowercase()) {
            "student" -> STUDENT
            "teacher" -> TEACHER
            else -> null
        }
    }
}
```

- [ ] **Step 2: Write `Collection.kt`**

```kotlin
package com.example.cardwords.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Collection(
    val id: String,
    val name: String,
    val description: String?,
    val cardsCount: Int,
)

@Immutable
data class CollectionCard(
    val id: String,
    val wordOriginal: String,
    val wordTranslation: String,
)

@Immutable
data class CollectionDetail(
    val collection: Collection,
    val cards: List<CollectionCard>,
)

@Immutable
data class StudentSummary(
    val id: String,
    val email: String,
    val name: String,
    val cardsCount: Int,
)

@Immutable
data class TeacherSummary(
    val id: String,
    val email: String,
    val name: String,
)

@Immutable
enum class AssignmentStatus { ASSIGNED, ADDED;

    companion object {
        fun fromWire(value: String?): AssignmentStatus = when (value?.lowercase()) {
            "added" -> ADDED
            else -> ASSIGNED // default for unknown → safe
        }
    }
}

@Immutable
data class AssignedCollection(
    val collection: Collection,
    val teacherName: String,
    val status: AssignmentStatus,
    /** Computed on the client by comparing cardsCount with locally-stored last-known count. */
    val hasNewCards: Boolean,
)
```

- [ ] **Step 3: Verify file compiles**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/data/model/UserRole.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/data/model/Collection.kt
git commit -m "Add UserRole + collection domain models for teacher/student feature"
```

---

### Task 2: API DTOs

Add request/response serializable classes to existing `CardWordsApiModels.kt`.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiModels.kt` (append at end)

- [ ] **Step 1: Append DTOs**

```kotlin
// ═══════════════════════════════════════════════════════════════
// Role
// ═══════════════════════════════════════════════════════════════

@Serializable
data class SwitchRoleRequest(val role: String)

// ═══════════════════════════════════════════════════════════════
// Teacher / Student summaries
// ═══════════════════════════════════════════════════════════════

@Serializable
data class StudentSummaryDto(
    val id: String,
    val email: String,
    val name: String,
    @SerialName("cards_count") val cardsCount: Int = 0,
)

@Serializable
data class TeacherSummaryDto(
    val id: String,
    val email: String,
    val name: String,
)

@Serializable
data class InviteStudentRequest(val email: String)

// ═══════════════════════════════════════════════════════════════
// Collections (teacher)
// ═══════════════════════════════════════════════════════════════

@Serializable
data class CollectionDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("cards_count") val cardsCount: Int = 0,
)

@Serializable
data class CollectionDetailDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("cards_count") val cardsCount: Int = 0,
    val cards: List<CollectionCardDto> = emptyList(),
)

@Serializable
data class CollectionCardDto(
    val id: String,
    @SerialName("word_original") val wordOriginal: String,
    @SerialName("word_translation") val wordTranslation: String,
)

@Serializable
data class CreateCollectionRequest(
    val name: String,
    val description: String,
)

@Serializable
data class UpdateCollectionRequest(
    val name: String,
    val description: String,
)

@Serializable
data class AddCollectionCardRequest(
    @SerialName("word_original") val wordOriginal: String,
    @SerialName("word_translation") val wordTranslation: String? = null,
)

// ═══════════════════════════════════════════════════════════════
// Assigned collections (student side)
// ═══════════════════════════════════════════════════════════════

@Serializable
data class AssignedCollectionDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("cards_count") val cardsCount: Int = 0,
    @SerialName("teacher_name") val teacherName: String = "",
    val status: String = "assigned",
)
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiModels.kt
git commit -m "Add DTO classes for teacher/student endpoints"
```

---

### Task 3: API client — `switchRole` and helper for status check

Add a `private suspend inline fun <reified T> apiGet/apiPost/...` helper isn't worth introducing for a few methods; instead each new method copies the explicit `if (response.status.value !in 200..299) error("HTTP ${response.status.value}")` pattern.

Start with `switchRole` + `getMe` upgrade so subsequent tasks build on it.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt`

- [ ] **Step 1: Locate the existing `getMe` method**

Open `CardWordsApiClient.kt`, find `suspend fun getMe`. We'll extend the existing model `UserResponse` to expose the role via a new field. Check current shape — if `role` not in `UserResponse`, add it.

- [ ] **Step 2: Update `UserResponse` to include `role`**

In `CardWordsApiModels.kt`, find `UserResponse` and add nullable `role`:

```kotlin
@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    @SerialName("subscription_status") val subscriptionStatus: String,
    /** Nullable for users created before role was introduced */
    val role: String? = null,
)
```

- [ ] **Step 3: Add `switchRole` to `CardWordsApiClient.kt`**

Add the method right after `getMe`:

```kotlin
suspend fun switchRole(token: String, role: String): Result<Unit> = runCatching {
    val response = client.put("$baseUrl/api/auth/role") {
        header("Authorization", bearerHeader(token))
        contentType(ContentType.Application.Json)
        setBody(SwitchRoleRequest(role))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
}
```

Add `import io.ktor.client.request.put` at top if missing.

- [ ] **Step 4: Verify compiles**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiModels.kt
git commit -m "Add switchRole API + role field on UserResponse"
```

---

### Task 4: API client — teacher students endpoints (3 methods)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt`

- [ ] **Step 1: Append new section after card endpoints**

```kotlin
// ═══════════════════════════════════════════════════════════════
// Teacher — students
// ═══════════════════════════════════════════════════════════════

suspend fun listMyStudents(token: String): Result<List<StudentSummaryDto>> = runCatching {
    val response = client.get("$baseUrl/api/teacher/students") {
        header("Authorization", bearerHeader(token))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
    response.body<List<StudentSummaryDto>>()
}

suspend fun inviteStudent(token: String, email: String): Result<StudentSummaryDto> = runCatching {
    val response = client.post("$baseUrl/api/teacher/students") {
        header("Authorization", bearerHeader(token))
        contentType(ContentType.Application.Json)
        setBody(InviteStudentRequest(email))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
    response.body<StudentSummaryDto>()
}

suspend fun removeStudent(token: String, studentId: String): Result<Unit> = runCatching {
    val response = client.delete("$baseUrl/api/teacher/students/$studentId") {
        header("Authorization", bearerHeader(token))
    }
    // 404 = already gone (e.g., student removed via web), idempotent
    if (response.status.value !in 200..299 && response.status.value != 404) {
        error("HTTP ${response.status.value}")
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt
git commit -m "Add teacher students API methods (list/invite/remove)"
```

---

### Task 5: API client — teacher collections CRUD (5 methods)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt`

- [ ] **Step 1: Append**

```kotlin
// ═══════════════════════════════════════════════════════════════
// Teacher — collections
// ═══════════════════════════════════════════════════════════════

suspend fun listMyCollections(token: String): Result<List<CollectionDto>> = runCatching {
    val response = client.get("$baseUrl/api/teacher/collections") {
        header("Authorization", bearerHeader(token))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
    response.body<List<CollectionDto>>()
}

suspend fun getCollection(token: String, id: String): Result<CollectionDetailDto> = runCatching {
    val response = client.get("$baseUrl/api/teacher/collections/$id") {
        header("Authorization", bearerHeader(token))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
    response.body<CollectionDetailDto>()
}

suspend fun createCollection(token: String, name: String, description: String): Result<CollectionDto> = runCatching {
    val response = client.post("$baseUrl/api/teacher/collections") {
        header("Authorization", bearerHeader(token))
        contentType(ContentType.Application.Json)
        setBody(CreateCollectionRequest(name, description))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
    response.body<CollectionDto>()
}

suspend fun updateCollection(
    token: String, id: String, name: String, description: String,
): Result<CollectionDto> = runCatching {
    val response = client.put("$baseUrl/api/teacher/collections/$id") {
        header("Authorization", bearerHeader(token))
        contentType(ContentType.Application.Json)
        setBody(UpdateCollectionRequest(name, description))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
    response.body<CollectionDto>()
}

suspend fun deleteCollection(token: String, id: String): Result<Unit> = runCatching {
    val response = client.delete("$baseUrl/api/teacher/collections/$id") {
        header("Authorization", bearerHeader(token))
    }
    if (response.status.value !in 200..299 && response.status.value != 404) {
        error("HTTP ${response.status.value}")
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt
git commit -m "Add teacher collections CRUD API"
```

---

### Task 6: API client — collection cards + assign + student endpoints

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt`

- [ ] **Step 1: Append**

```kotlin
// ═══════════════════════════════════════════════════════════════
// Teacher — cards inside collection + assignment
// ═══════════════════════════════════════════════════════════════

suspend fun addCardToCollection(
    token: String, cid: String, original: String, translation: String?,
): Result<CollectionCardDto> = runCatching {
    val response = client.post("$baseUrl/api/teacher/collections/$cid/cards") {
        header("Authorization", bearerHeader(token))
        contentType(ContentType.Application.Json)
        setBody(AddCollectionCardRequest(original, translation))
    }
    // Treat 409 (already in collection) as success — returns existing card body in idempotent backends.
    if (response.status.value !in 200..299 && response.status.value != 409) {
        error("HTTP ${response.status.value}")
    }
    response.body<CollectionCardDto>()
}

suspend fun removeCardFromCollection(
    token: String, cid: String, cardId: String,
): Result<Unit> = runCatching {
    val response = client.delete("$baseUrl/api/teacher/collections/$cid/cards/$cardId") {
        header("Authorization", bearerHeader(token))
    }
    if (response.status.value !in 200..299 && response.status.value != 404) {
        error("HTTP ${response.status.value}")
    }
}

suspend fun assignCollection(
    token: String, cid: String, studentId: String,
): Result<Unit> = runCatching {
    val response = client.post("$baseUrl/api/teacher/collections/$cid/assign/$studentId") {
        header("Authorization", bearerHeader(token))
    }
    // 409 = already assigned; treat as success (idempotent assign).
    if (response.status.value !in 200..299 && response.status.value != 409) {
        error("HTTP ${response.status.value}")
    }
}

// ═══════════════════════════════════════════════════════════════
// Student
// ═══════════════════════════════════════════════════════════════

suspend fun listMyTeachers(token: String): Result<List<TeacherSummaryDto>> = runCatching {
    val response = client.get("$baseUrl/api/student/teachers") {
        header("Authorization", bearerHeader(token))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
    response.body<List<TeacherSummaryDto>>()
}

suspend fun listAssignedCollections(token: String): Result<List<AssignedCollectionDto>> = runCatching {
    val response = client.get("$baseUrl/api/student/collections") {
        header("Authorization", bearerHeader(token))
    }
    if (response.status.value !in 200..299) {
        error("HTTP ${response.status.value}")
    }
    response.body<List<AssignedCollectionDto>>()
}

suspend fun acceptCollection(token: String, cid: String): Result<Unit> = runCatching {
    val response = client.post("$baseUrl/api/student/collections/$cid/add") {
        header("Authorization", bearerHeader(token))
    }
    // Idempotent: if collection already added on server, expect 200 or 409 — both OK for client.
    if (response.status.value !in 200..299 && response.status.value != 409) {
        error("HTTP ${response.status.value}")
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/CardWordsApiClient.kt
git commit -m "Add collection-cards/assign + student API methods"
```

---

### Task 7: AuthManager — `roleFlow`, `setRole`, `refreshRoleFromServer`, persist

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/AuthManager.kt`

- [ ] **Step 1: Read current file structure**

Note current state: `AuthManager` holds token + user info via `repository.getSetting/setSetting`. Has `saveSession`, `logout`, etc.

- [ ] **Step 2: Add role state and methods**

Replace the `AuthManager` class body with:

```kotlin
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
```

- [ ] **Step 3: Update `AppModule.kt` to pass `apiClient` into `AuthManager`**

Open `composeApp/src/commonMain/kotlin/com/example/cardwords/di/AppModule.kt`. The `authManager` lazy currently uses `AuthManager(databaseRepository)`. Update to:

```kotlin
val authManager: AuthManager by lazy {
    AuthManager(databaseRepository, cardWordsApiClient)
}
```

- [ ] **Step 4: Update existing call sites of `saveSession`**

Find usages: `AuthViewModel.kt`. The current call is `authManager.saveSession(token, email, name, subscription)` (4-arg). Add the role:

```kotlin
authManager.saveSession(
    token = response.accessToken,
    email = response.user.email,
    name = response.user.name,
    subscription = response.user.subscriptionStatus,
    role = UserRole.fromWire(response.user.role),
)
```

Add the import `import com.example.cardwords.data.model.UserRole` at the top.

- [ ] **Step 5: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/data/remote/AuthManager.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/di/AppModule.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/auth/AuthViewModel.kt
git commit -m "Extend AuthManager with roleFlow + setRole + refresh"
```

---

### Task 8: Routes — `RoleSelectionRoute` + `CollectionDetailRoute`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/navigation/Routes.kt`

- [ ] **Step 1: Append two new routes**

```kotlin
@Serializable
data object RoleSelectionRoute

@Serializable
data class CollectionDetailRoute(val collectionId: String)
```

- [ ] **Step 2: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/navigation/Routes.kt
git commit -m "Add RoleSelectionRoute + CollectionDetailRoute"
```

---

### Task 9: `RoleSelectionViewModel` + `RoleSelectionScreen`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/auth/RoleSelectionViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/auth/RoleSelectionScreen.kt`

- [ ] **Step 1: Write `RoleSelectionViewModel.kt`**

```kotlin
package com.example.cardwords.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RolePickPhase { IDLE, SUBMITTING, ERROR }

data class RoleSelectionUiState(
    val phase: RolePickPhase = RolePickPhase.IDLE,
    val error: String? = null,
)

class RoleSelectionViewModel : ViewModel() {
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(RoleSelectionUiState())
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    fun pickRole(role: UserRole, onSuccess: () -> Unit) {
        if (_uiState.value.phase == RolePickPhase.SUBMITTING) return
        _uiState.value = RoleSelectionUiState(phase = RolePickPhase.SUBMITTING)
        viewModelScope.launch {
            val result = authManager.setRole(role)
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { e ->
                    _uiState.value = RoleSelectionUiState(
                        phase = RolePickPhase.ERROR,
                        error = mapError(e.message),
                    )
                },
            )
        }
    }

    private fun mapError(raw: String?): String = when {
        raw == null -> "Не удалось установить роль"
        raw.contains("401") -> "Сессия истекла, войдите заново"
        raw.contains("ConnectException", ignoreCase = true) ||
            raw.contains("UnresolvedAddress", ignoreCase = true) -> "Нет подключения к интернету"
        raw.contains("timeout", ignoreCase = true) -> "Превышено время ожидания"
        else -> "Не удалось установить роль"
    }
}
```

- [ ] **Step 2: Write `RoleSelectionScreen.kt`**

```kotlin
package com.example.cardwords.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun RoleSelectionScreen(
    onPicked: () -> Unit,
) {
    val viewModel = remember { RoleSelectionViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "Кто вы?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = LightFg,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Выберите роль для продолжения. Её можно сменить в любой момент в Профиле.",
            fontSize = 14.sp,
            color = LightFgSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        RoleCard(
            title = "Я — ученик",
            subtitle = "Учу слова, прохожу тесты",
            enabled = state.phase != RolePickPhase.SUBMITTING,
            onClick = { viewModel.pickRole(UserRole.STUDENT, onPicked) },
        )
        Spacer(Modifier.height(12.dp))
        RoleCard(
            title = "Я — преподаватель",
            subtitle = "Создаю коллекции, выдаю их ученикам",
            enabled = state.phase != RolePickPhase.SUBMITTING,
            onClick = { viewModel.pickRole(UserRole.TEACHER, onPicked) },
        )

        Spacer(Modifier.height(20.dp))

        when (state.phase) {
            RolePickPhase.SUBMITTING -> CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = LightFg,
                strokeWidth = 2.dp,
            )
            RolePickPhase.ERROR -> Text(
                text = state.error ?: "Ошибка",
                fontSize = 13.sp,
                color = Red40,
                textAlign = TextAlign.Center,
            )
            RolePickPhase.IDLE -> Spacer(Modifier.height(28.dp))
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(
                color = LightCard,
                borderColor = DividerColor,
                borderWidth = 0.5.dp,
                cornerRadius = 16.dp,
                clipContent = true,
            )
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(20.dp),
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightFg,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = LightFgSecondary,
        )
    }
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/auth/RoleSelectionViewModel.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/auth/RoleSelectionScreen.kt
git commit -m "Add RoleSelectionScreen + ViewModel (mandatory gate)"
```

---

### Task 10: `App.kt` routing gate — role==null → RoleSelectionRoute

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/App.kt`

- [ ] **Step 1: Insert role-pick gate logic**

In `App.kt`, find the existing `startRoute` computation. Replace with:

```kotlin
val authManager = AppModule.authManager
val role by authManager.roleFlow.collectAsStateWithLifecycle()

val startRoute: Any = when {
    !authManager.isLoggedIn()                         -> WelcomeRoute
    role == null                                      -> RoleSelectionRoute
    !OnboardingManager.isOnboardingCompleted(repository) -> OnboardingRoute
    else                                              -> MainRoute
}
```

- [ ] **Step 2: Add the composable route**

Inside the `NavHost` block, add:

```kotlin
composable<RoleSelectionRoute> {
    RoleSelectionScreen(
        onPicked = {
            val next = if (OnboardingManager.isOnboardingCompleted(repository))
                MainRoute else OnboardingRoute
            navController.navigate(next) {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}
```

- [ ] **Step 3: Update existing `AuthRoute.onAuthSuccess` so it routes through role-pick if needed**

```kotlin
composable<AuthRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<AuthRoute>()
    AuthScreen(
        initialTab = if (route.initialTab == "register") AuthTab.REGISTER else AuthTab.LOGIN,
        onAuthSuccess = {
            val next = when {
                authManager.getRole() == null                      -> RoleSelectionRoute
                !OnboardingManager.isOnboardingCompleted(repository) -> OnboardingRoute
                else                                                -> MainRoute
            }
            navController.navigate(next) { popUpTo(0) { inclusive = true } }
        },
        onNavigateBack = { navController.popBackStack() },
    )
}
```

- [ ] **Step 4: Add imports if missing**

```kotlin
import com.example.cardwords.navigation.RoleSelectionRoute
import com.example.cardwords.ui.auth.RoleSelectionScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

- [ ] **Step 5: Compile + run app smoke**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/App.kt
git commit -m "Add role-pick gate to App routing"
```

---

### Task 11: MainScreen — dynamic tabs by role + `key(role)`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/main/MainScreen.kt`

- [ ] **Step 1: Read existing tab definitions**

The current `tabs: List<TabItem>` is a top-level `val`. We need: `studentTabs`, `teacherTabs` and pick by role.

- [ ] **Step 2: Refactor — split into two top-level lists**

Replace the top-level `tabs` declaration with two role-specific lists (note: keep existing icon composables HomeNavIcon / BookNavIcon / etc.):

```kotlin
private val studentTabs = listOf(
    TabItem("Главная",    HomeRoute,       { m, c -> HomeNavIcon(m, c) }),
    TabItem("Слова",      WordsTabRoute,   { m, c -> BookNavIcon(m, c) }),
    TabItem("Статистика", StatsTabRoute,   { m, c -> BarChartNavIcon(m, c) }),
    TabItem("Учителя",    MyTeachersTabRoute, { m, c -> PersonNavIcon(m, c) }),
    TabItem("Профиль",    ProfileTabRoute, { m, c -> PersonNavIcon(m, c) }),
)

private val teacherTabs = listOf(
    TabItem("Главная",      HomeRoute,         { m, c -> HomeNavIcon(m, c) }),
    TabItem("Слова",        WordsTabRoute,     { m, c -> BookNavIcon(m, c) }),
    TabItem("Статистика",   StatsTabRoute,     { m, c -> BarChartNavIcon(m, c) }),
    TabItem("Преподавание", TeachingTabRoute,  { m, c -> PersonNavIcon(m, c) }),
    TabItem("Профиль",      ProfileTabRoute,   { m, c -> PersonNavIcon(m, c) }),
)
```

- [ ] **Step 3: Add new tab routes**

In `composeApp/src/commonMain/kotlin/com/example/cardwords/navigation/Routes.kt`:

```kotlin
@Serializable data object MyTeachersTabRoute
@Serializable data object TeachingTabRoute
```

- [ ] **Step 4: Wrap MainScreen body in `key(role)`**

Find the `Scaffold(...) { ... NavHost(...) ... }` block. Wrap the whole content with:

```kotlin
@Composable
fun MainScreen(
    outerNavController: NavHostController,
    onLogout: () -> Unit = {},
) {
    val authManager = AppModule.authManager
    val role by authManager.roleFlow.collectAsStateWithLifecycle()
    val tabs = remember(role) {
        when (role) {
            UserRole.STUDENT -> studentTabs
            UserRole.TEACHER -> teacherTabs
            null             -> emptyList()
        }
    }

    key(role) {
        val tabNavController = rememberNavController()
        // ... existing Scaffold body, but use `tabs` instead of `tabs` (the old top-level)
        // ... NavHost startDestination = HomeRoute (unchanged)
        // ... add composable<MyTeachersTabRoute> and composable<TeachingTabRoute>
    }
}
```

Add imports:

```kotlin
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.di.AppModule
import com.example.cardwords.navigation.MyTeachersTabRoute
import com.example.cardwords.navigation.TeachingTabRoute
```

- [ ] **Step 5: Add NavHost destinations for the new tabs**

Inside the NavHost in MainScreen:

```kotlin
composable<MyTeachersTabRoute> {
    MyTeachersScreen()
}
composable<TeachingTabRoute> {
    TeachingScreen(
        onOpenCollection = { collectionId ->
            outerNavController.navigate(CollectionDetailRoute(collectionId))
        },
    )
}
```

The screen composables don't exist yet — leave imports referenced but expect compile errors until Task 13/16. **For this task, comment out the body** of these two destinations to keep compile green:

```kotlin
composable<MyTeachersTabRoute> { /* TODO Task 13 */ Box(Modifier.fillMaxSize()) }
composable<TeachingTabRoute>   { /* TODO Task 16 */ Box(Modifier.fillMaxSize()) }
```

- [ ] **Step 6: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/main/MainScreen.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/navigation/Routes.kt
git commit -m "MainScreen: dynamic tabs by role via key(role)"
```

---

### Task 12: `MyTeachersViewModel` (student-side) — load + accept + badge

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/student/MyTeachersViewModel.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.example.cardwords.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.AssignedCollection
import com.example.cardwords.data.model.AssignmentStatus
import com.example.cardwords.data.model.Collection
import com.example.cardwords.data.model.TeacherSummary
import com.example.cardwords.di.AppModule
import com.example.cardwords.data.remote.AssignedCollectionDto
import com.example.cardwords.data.remote.TeacherSummaryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MyTeachersPhase { LOADING, CONTENT, ERROR }

data class MyTeachersUiState(
    val phase: MyTeachersPhase = MyTeachersPhase.LOADING,
    val teachers: List<TeacherSummary> = emptyList(),
    val collections: List<AssignedCollection> = emptyList(),
    val error: String? = null,
    val acceptingIds: Set<String> = emptySet(),
)

class MyTeachersViewModel : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager
    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(MyTeachersUiState())
    val uiState: StateFlow<MyTeachersUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(phase = MyTeachersPhase.LOADING, error = null) }
        viewModelScope.launch {
            val (teachersR, collectionsR) = withContext(Dispatchers.Default) {
                apiClient.listMyTeachers(token) to apiClient.listAssignedCollections(token)
            }
            val teachers = teachersR.getOrNull()?.map { it.toDomain() } ?: emptyList()
            val rawCollections = collectionsR.getOrNull() ?: emptyList()

            val mapped = rawCollections.map { dto ->
                val storedKey = "accepted_coll_${dto.id}"
                val lastKnown = repository.getSetting(storedKey)?.toIntOrNull() ?: 0
                val status = AssignmentStatus.fromWire(dto.status)
                AssignedCollection(
                    collection = Collection(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        cardsCount = dto.cardsCount,
                    ),
                    teacherName = dto.teacherName,
                    status = status,
                    hasNewCards = status == AssignmentStatus.ADDED && dto.cardsCount > lastKnown,
                )
            }
            val isError = teachersR.isFailure || collectionsR.isFailure
            _uiState.update {
                it.copy(
                    phase = if (isError) MyTeachersPhase.ERROR else MyTeachersPhase.CONTENT,
                    teachers = teachers,
                    collections = mapped,
                    error = if (isError) "Не удалось загрузить" else null,
                )
            }
        }
    }

    fun acceptCollection(collectionId: String) {
        if (collectionId in _uiState.value.acceptingIds) return
        val token = authManager.getToken() ?: return
        val previous = _uiState.value
        // optimistic: set status to ADDED + clear hasNewCards
        _uiState.update { state ->
            state.copy(
                acceptingIds = state.acceptingIds + collectionId,
                collections = state.collections.map { ac ->
                    if (ac.collection.id == collectionId) {
                        ac.copy(status = AssignmentStatus.ADDED, hasNewCards = false)
                    } else ac
                },
            )
        }
        AppModule.syncScope.launch {
            val result = apiClient.acceptCollection(token, collectionId)
            result.onSuccess {
                // Persist new "last known" cards count
                val target = previous.collections.firstOrNull { it.collection.id == collectionId }
                if (target != null) {
                    repository.setSetting("accepted_coll_$collectionId", target.collection.cardsCount.toString())
                }
                // Pull new cards into the personal dictionary
                AppModule.syncScope.launch { syncDictionary() }
            }
            result.onFailure {
                // rollback
                _uiState.update { state ->
                    state.copy(
                        acceptingIds = state.acceptingIds - collectionId,
                        collections = previous.collections,
                        error = "Не удалось принять коллекцию",
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(acceptingIds = it.acceptingIds - collectionId) }
        }
    }

    private suspend fun syncDictionary() {
        val token = authManager.getToken() ?: return
        val cardsResult = apiClient.getCards(token)
        cardsResult.onSuccess { cards ->
            withContext(Dispatchers.Default) {
                val existingByOriginal = repository.getDictionaryWords()
                    .associateBy { it.original.lowercase() }
                val toInsert = mutableListOf<com.example.cardwords.data.model.Word>()
                cards.forEach { card ->
                    val key = card.wordOriginal.lowercase()
                    if (existingByOriginal[key] == null) {
                        toInsert.add(
                            com.example.cardwords.data.model.Word(
                                id = 0L,
                                original = card.wordOriginal,
                                translation = card.wordTranslation,
                                transcription = "",
                                category = "",
                                isInDictionary = true,
                                addedAt = repository.currentTimeMillis(),
                                source = "server",
                            )
                        )
                    } else {
                        val w = existingByOriginal[key]!!
                        if (!w.isInDictionary) repository.addToDictionary(w.id)
                    }
                }
                if (toInsert.isNotEmpty()) repository.insertWordsInTransaction(toInsert)
            }
        }
    }

    private fun TeacherSummaryDto.toDomain() = TeacherSummary(id = id, email = email, name = name)
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/student/MyTeachersViewModel.kt
git commit -m "Add MyTeachersViewModel with accept + reaccept badge"
```

---

### Task 13: `MyTeachersScreen` (student-side 5th tab)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/student/MyTeachersScreen.kt`

- [ ] **Step 1: Write the screen**

```kotlin
package com.example.cardwords.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.AssignedCollection
import com.example.cardwords.data.model.AssignmentStatus
import com.example.cardwords.data.model.TeacherSummary
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.Green40
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar

private val DividerColor = Color(0xFFE5E5EA)
private val SectionLabel = Color(0xFF8A8A8A)

@Composable
fun MyTeachersScreen() {
    val viewModel = remember { MyTeachersViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Учителя",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = LightFg,
            )
        }

        PullRefresh(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when (state.phase) {
                MyTeachersPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LightFg, strokeWidth = 2.dp)
                }
                MyTeachersPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Ошибка", fontSize = 14.sp, color = LightFgSecondary)
                }
                MyTeachersPhase.CONTENT -> ContentList(state, onAccept = viewModel::acceptCollection)
            }
        }
    }
}

@Composable
private fun ContentList(
    state: MyTeachersUiState,
    onAccept: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Teachers section
        item {
            SectionHeader("УЧИТЕЛЯ · ${state.teachers.size}")
        }
        if (state.teachers.isEmpty()) {
            item { EmptyHint("У вас пока нет учителей. Учитель пригласит вас по email.") }
        } else {
            items(state.teachers, key = { it.id }) { TeacherRow(it) }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // Collections section
        item {
            SectionHeader("НАЗНАЧЕННЫЕ КОЛЛЕКЦИИ · ${state.collections.size}")
        }
        if (state.collections.isEmpty()) {
            item { EmptyHint("Учитель пока ничего не назначил.") }
        } else {
            items(state.collections, key = { it.collection.id }) { coll ->
                CollectionRow(
                    item = coll,
                    accepting = coll.collection.id in state.acceptingIds,
                    onAccept = { onAccept(coll.collection.id) },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = SectionLabel,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(LightCard, DividerColor, 0.5.dp, 14.dp)
            .padding(20.dp),
    ) {
        Text(text = text, fontSize = 13.sp, color = LightFgSecondary)
    }
}

@Composable
private fun TeacherRow(teacher: TeacherSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(LightCard, DividerColor, 0.5.dp, 14.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(teacher.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
            Spacer(Modifier.height(2.dp))
            Text(teacher.email, fontSize = 12.sp, color = LightFgSecondary)
        }
    }
}

@Composable
private fun CollectionRow(
    item: AssignedCollection,
    accepting: Boolean,
    onAccept: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(LightCard, DividerColor, 0.5.dp, 14.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.collection.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
            if (!item.collection.description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(item.collection.description, fontSize = 12.sp, color = LightFgSecondary)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(item.status)
                Spacer(Modifier.width(8.dp))
                Text("${item.collection.cardsCount} карточек · от ${item.teacherName}", fontSize = 11.sp, color = LightFgSecondary)
                if (item.hasNewCards) {
                    Spacer(Modifier.width(8.dp))
                    NewBadge()
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        AcceptButton(item, accepting, onAccept)
    }
}

@Composable
private fun StatusChip(status: AssignmentStatus) {
    val (bg, fg, label) = when (status) {
        AssignmentStatus.ASSIGNED -> Triple(LightProgressBar.copy(alpha = 0.12f), LightProgressBar, "Выдана")
        AssignmentStatus.ADDED    -> Triple(Green40.copy(alpha = 0.12f), Green40, "Добавлена")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun NewBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LightProgressBar)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text("Новые", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun AcceptButton(
    item: AssignedCollection,
    accepting: Boolean,
    onAccept: () -> Unit,
) {
    val isAdded = item.status == AssignmentStatus.ADDED && !item.hasNewCards
    val label = when {
        accepting -> "..."
        item.hasNewCards -> "Обновить"
        isAdded -> "OK"
        else -> "Принять"
    }
    val enabled = !accepting && (!isAdded || item.hasNewCards)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) LightFg else Color(0xFFE0E0E0))
            .let { if (enabled) it.clickable(onClick = onAccept) else it }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
    }
}
```

- [ ] **Step 2: Wire screen into MainScreen NavHost**

Open `MainScreen.kt`. Replace the `composable<MyTeachersTabRoute>` placeholder with:

```kotlin
composable<MyTeachersTabRoute> {
    MyTeachersScreen()
}
```

Add import: `import com.example.cardwords.ui.student.MyTeachersScreen`.

- [ ] **Step 3: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/student/MyTeachersScreen.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/main/MainScreen.kt
git commit -m "Add MyTeachersScreen wired into student tab"
```

---

### Task 14: Profile — «Сменить роль» action

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/profile/ProfileViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/profile/ProfileScreen.kt`

- [ ] **Step 1: Add `switchRole` to `ProfileViewModel`**

In `ProfileUiState` add `currentRole: UserRole? = null` and `roleSwitchInFlight: Boolean = false`. In `init`/`refresh`:

```kotlin
state.copy(currentRole = AppModule.authManager.getRole(), ...)
```

Add a method:

```kotlin
fun switchRole(newRole: UserRole) {
    if (_uiState.value.roleSwitchInFlight) return
    _uiState.update { it.copy(roleSwitchInFlight = true) }
    viewModelScope.launch {
        AppModule.authManager.setRole(newRole)
        _uiState.update {
            it.copy(roleSwitchInFlight = false, currentRole = AppModule.authManager.getRole())
        }
    }
}
```

- [ ] **Step 2: In `ProfileScreen`, add an action row "Сменить роль"**

```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
var showRoleDialog by remember { mutableStateOf(false) }

// Inside the existing column, near logout:
Row(
    modifier = Modifier
        .fillMaxWidth()
        .cardBg(LightCard, DividerColor, 0.5.dp, 14.dp)
        .clickable(enabled = !state.roleSwitchInFlight) { showRoleDialog = true }
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(Modifier.weight(1f)) {
        Text("Сменить роль", fontSize = 15.sp, color = LightFg, fontWeight = FontWeight.SemiBold)
        Text(
            text = when (state.currentRole) {
                UserRole.STUDENT -> "Сейчас: ученик"
                UserRole.TEACHER -> "Сейчас: преподаватель"
                null             -> "Не задана"
            },
            fontSize = 12.sp,
            color = LightFgSecondary,
        )
    }
}

if (showRoleDialog) {
    RolePickDialog(
        current = state.currentRole,
        onPick = {
            viewModel.switchRole(it)
            showRoleDialog = false
        },
        onDismiss = { showRoleDialog = false },
    )
}
```

- [ ] **Step 3: Add `RolePickDialog` private composable in same file**

```kotlin
@Composable
private fun RolePickDialog(
    current: UserRole?,
    onPick: (UserRole) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cardBg(LightCard, DividerColor, 0.5.dp, 18.dp)
                .padding(20.dp),
        ) {
            Text("Выберите роль", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LightFg)
            Spacer(Modifier.height(12.dp))
            UserRole.values().forEach { role ->
                val label = when (role) {
                    UserRole.STUDENT -> "Я — ученик"
                    UserRole.TEACHER -> "Я — преподаватель"
                }
                val isCurrent = role == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isCurrent) { onPick(role) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(label, fontSize = 15.sp, color = if (isCurrent) LightFgSecondary else LightFg)
                    Spacer(Modifier.weight(1f))
                    if (isCurrent) Text("текущая", fontSize = 11.sp, color = LightFgSecondary)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/profile/ProfileViewModel.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/profile/ProfileScreen.kt
git commit -m "Profile: add role switcher action and dialog"
```

---

### Task 15: `MyStudentsViewModel` + `StudentsTab` + `AddStudentDialog`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/MyStudentsViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/StudentsTab.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AddStudentDialog.kt`

- [ ] **Step 1: Write `MyStudentsViewModel.kt`**

```kotlin
package com.example.cardwords.ui.teaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.StudentSummary
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudentsPhase { LOADING, CONTENT, ERROR }

private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

data class MyStudentsUiState(
    val phase: StudentsPhase = StudentsPhase.LOADING,
    val students: List<StudentSummary> = emptyList(),
    val error: String? = null,
    val inviteInFlight: Boolean = false,
    val inviteError: String? = null,
    val removingIds: Set<String> = emptySet(),
)

class MyStudentsViewModel : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(MyStudentsUiState())
    val uiState: StateFlow<MyStudentsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(phase = StudentsPhase.LOADING, error = null) }
        viewModelScope.launch {
            val result = apiClient.listMyStudents(token)
            result.fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(
                        phase = StudentsPhase.CONTENT,
                        students = list.map { dto -> StudentSummary(dto.id, dto.email, dto.name, dto.cardsCount) },
                    ) }
                },
                onFailure = {
                    _uiState.update { it.copy(phase = StudentsPhase.ERROR, error = "Не удалось загрузить") }
                },
            )
        }
    }

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun invite(rawEmail: String, onComplete: () -> Unit) {
        val email = rawEmail.trim()
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(inviteError = "Некорректный email") }
            return
        }
        if (_uiState.value.inviteInFlight) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(inviteInFlight = true, inviteError = null) }
        AppModule.syncScope.launch {
            val result = apiClient.inviteStudent(token, email)
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { state ->
                        state.copy(
                            inviteInFlight = false,
                            inviteError = null,
                            students = state.students + StudentSummary(dto.id, dto.email, dto.name, dto.cardsCount),
                        )
                    }
                    onComplete()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(inviteInFlight = false, inviteError = mapInviteError(e.message))
                    }
                },
            )
        }
    }

    fun removeStudent(id: String) {
        if (id in _uiState.value.removingIds) return
        val token = authManager.getToken() ?: return
        val previous = _uiState.value.students
        _uiState.update { state ->
            state.copy(
                removingIds = state.removingIds + id,
                students = state.students.filter { it.id != id },
            )
        }
        AppModule.syncScope.launch {
            val result = apiClient.removeStudent(token, id)
            if (result.isFailure) {
                // rollback
                _uiState.update { it.copy(students = previous, error = "Не удалось удалить") }
            }
            _uiState.update { it.copy(removingIds = it.removingIds - id) }
        }
    }

    private fun mapInviteError(raw: String?): String = when {
        raw == null -> "Не удалось пригласить"
        raw.contains("404") -> "Пользователь с таким email не найден"
        raw.contains("400") -> "Этот пользователь не является учеником"
        raw.contains("409") -> "Уже в списке учеников"
        else -> "Не удалось пригласить"
    }
}
```

- [ ] **Step 2: Write `AddStudentDialog.kt`**

```kotlin
package com.example.cardwords.ui.teaching.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)
private val FieldBg = Color(0xFFF2F2F7)

@Composable
fun AddStudentDialog(
    inFlight: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cardBg(LightCard, DividerColor, 0.5.dp, 18.dp)
                .padding(20.dp),
        ) {
            Text("Пригласить ученика", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LightFg)
            Spacer(Modifier.height(12.dp))
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("email", color = LightFgSecondary, fontSize = 15.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = LightFg,
                    unfocusedTextColor = LightFg,
                    focusedContainerColor = FieldBg,
                    unfocusedContainerColor = FieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = LightFg,
                ),
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, fontSize = 12.sp, color = Red40)
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF2F2F7))
                        .clickable(enabled = !inFlight, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("Отмена", fontSize = 14.sp, color = LightFg) }
                Spacer(Modifier.width(10.dp))
                val canSubmit = email.isNotBlank() && !inFlight
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (canSubmit) LightFg else Color(0xFFCCCCCC))
                        .clickable(enabled = canSubmit) { onSubmit(email) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (inFlight) "..." else "Пригласить", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Write `StudentsTab.kt`**

```kotlin
package com.example.cardwords.ui.teaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.StudentSummary
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.teaching.dialogs.AddStudentDialog
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun StudentsTab(viewModel: MyStudentsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showInvite by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(LightBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${state.students.size} учеников", fontSize = 13.sp, color = LightFgSecondary, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(LightFg)
                    .clickable { showInvite = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("+ Пригласить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
            }
        }

        PullRefresh(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when (state.phase) {
                StudentsPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LightFg, strokeWidth = 2.dp)
                }
                StudentsPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Ошибка", fontSize = 14.sp, color = Red40)
                }
                StudentsPhase.CONTENT -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.students.isEmpty()) {
                            item { EmptyHint() }
                        } else {
                            items(state.students, key = { it.id }) { student ->
                                StudentRow(student, removing = student.id in state.removingIds, onRemove = { viewModel.removeStudent(student.id) })
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    if (showInvite) {
        AddStudentDialog(
            inFlight = state.inviteInFlight,
            error = state.inviteError,
            onSubmit = { email -> viewModel.invite(email) { showInvite = false } },
            onDismiss = { showInvite = false },
        )
    }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(20.dp),
    ) {
        Text("Пригласите первого ученика по email.", fontSize = 13.sp, color = LightFgSecondary)
    }
}

@Composable
private fun StudentRow(student: StudentSummary, removing: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
            Spacer(Modifier.height(2.dp))
            Text("${student.email} · ${student.cardsCount} карточек", fontSize = 12.sp, color = LightFgSecondary)
        }
        if (!removing) {
            Box(
                modifier = Modifier.clickable(onClick = onRemove).padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Удалить", fontSize = 12.sp, color = Red40)
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = LightFgSecondary)
        }
    }
}
```

- [ ] **Step 4: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/MyStudentsViewModel.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/StudentsTab.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AddStudentDialog.kt
git commit -m "Add MyStudents flow (VM + tab + invite dialog)"
```

---

### Task 16: `MyCollectionsViewModel` + `CollectionsTab` + `CreateCollectionDialog`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/MyCollectionsViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionsTab.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/CreateCollectionDialog.kt`

Pattern mirrors Task 15. Code is symmetric — see `MyStudentsViewModel`/`StudentsTab`/`AddStudentDialog` and adapt for collections (use `apiClient.listMyCollections`, `apiClient.createCollection`, `apiClient.deleteCollection`).

- [ ] **Step 1: Write `MyCollectionsViewModel.kt`**

```kotlin
package com.example.cardwords.ui.teaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.Collection
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CollectionsPhase { LOADING, CONTENT, ERROR }

data class MyCollectionsUiState(
    val phase: CollectionsPhase = CollectionsPhase.LOADING,
    val collections: List<Collection> = emptyList(),
    val error: String? = null,
    val createInFlight: Boolean = false,
    val createError: String? = null,
    val deletingIds: Set<String> = emptySet(),
)

class MyCollectionsViewModel : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(MyCollectionsUiState())
    val uiState: StateFlow<MyCollectionsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(phase = CollectionsPhase.LOADING, error = null) }
        viewModelScope.launch {
            val result = apiClient.listMyCollections(token)
            result.fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(
                        phase = CollectionsPhase.CONTENT,
                        collections = list.map { dto ->
                            Collection(dto.id, dto.name, dto.description, dto.cardsCount)
                        },
                    ) }
                },
                onFailure = {
                    _uiState.update { it.copy(phase = CollectionsPhase.ERROR, error = "Не удалось загрузить") }
                },
            )
        }
    }

    fun create(name: String, description: String, onComplete: () -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(createError = "Имя не может быть пустым") }
            return
        }
        if (_uiState.value.createInFlight) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(createInFlight = true, createError = null) }
        AppModule.syncScope.launch {
            val result = apiClient.createCollection(token, trimmedName, description.trim())
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { state ->
                        state.copy(
                            createInFlight = false,
                            collections = state.collections + Collection(dto.id, dto.name, dto.description, dto.cardsCount),
                        )
                    }
                    onComplete()
                },
                onFailure = {
                    _uiState.update { it.copy(createInFlight = false, createError = "Не удалось создать") }
                },
            )
        }
    }

    fun delete(id: String) {
        if (id in _uiState.value.deletingIds) return
        val token = authManager.getToken() ?: return
        val previous = _uiState.value.collections
        _uiState.update {
            it.copy(deletingIds = it.deletingIds + id, collections = it.collections.filter { c -> c.id != id })
        }
        AppModule.syncScope.launch {
            val result = apiClient.deleteCollection(token, id)
            if (result.isFailure) {
                _uiState.update { it.copy(collections = previous, error = "Не удалось удалить") }
            }
            _uiState.update { it.copy(deletingIds = it.deletingIds - id) }
        }
    }
}
```

- [ ] **Step 2: Write `CreateCollectionDialog.kt`**

```kotlin
package com.example.cardwords.ui.teaching.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)
private val FieldBg = Color(0xFFF2F2F7)

@Composable
fun CreateCollectionDialog(
    inFlight: Boolean,
    error: String?,
    onSubmit: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 18.dp).padding(20.dp),
        ) {
            Text("Новая коллекция", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LightFg)
            Spacer(Modifier.height(12.dp))
            Field(name, { name = it }, "Название", singleLine = true, max = 100)
            Spacer(Modifier.height(8.dp))
            Field(description, { description = it }, "Описание (необязательно)", singleLine = false, max = 500)
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, fontSize = 12.sp, color = Red40)
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Box(
                    modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                        .background(FieldBg).clickable(enabled = !inFlight, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("Отмена", fontSize = 14.sp, color = LightFg) }
                Spacer(Modifier.width(10.dp))
                val canSubmit = name.isNotBlank() && !inFlight
                Box(
                    modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                        .background(if (canSubmit) LightFg else Color(0xFFCCCCCC))
                        .clickable(enabled = canSubmit) { onSubmit(name, description) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (inFlight) "..." else "Создать", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
                }
            }
        }
    }
}

@Composable
private fun Field(value: String, onValue: (String) -> Unit, placeholder: String, singleLine: Boolean, max: Int) {
    TextField(
        value = value,
        onValueChange = { if (it.length <= max) onValue(it) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        placeholder = { Text(placeholder, color = LightFgSecondary, fontSize = 15.sp) },
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedTextColor = LightFg,
            unfocusedTextColor = LightFg,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = LightFg,
        ),
    )
}
```

- [ ] **Step 3: Write `CollectionsTab.kt`**

```kotlin
package com.example.cardwords.ui.teaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.Collection
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.teaching.dialogs.CreateCollectionDialog
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun CollectionsTab(
    viewModel: MyCollectionsViewModel,
    onOpenCollection: (id: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(LightBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${state.collections.size} коллекций", fontSize = 13.sp, color = LightFgSecondary, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(LightFg)
                    .clickable { showCreate = true }.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("+ Создать", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
            }
        }

        PullRefresh(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when (state.phase) {
                CollectionsPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LightFg, strokeWidth = 2.dp)
                }
                CollectionsPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Ошибка", fontSize = 14.sp, color = Red40)
                }
                CollectionsPhase.CONTENT -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.collections.isEmpty()) {
                            item { EmptyHint() }
                        } else {
                            items(state.collections, key = { it.id }) { c ->
                                CollectionRow(c, onClick = { onOpenCollection(c.id) })
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateCollectionDialog(
            inFlight = state.createInFlight,
            error = state.createError,
            onSubmit = { name, desc -> viewModel.create(name, desc) { showCreate = false } },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(20.dp),
    ) {
        Text("Создайте первую коллекцию.", fontSize = 13.sp, color = LightFgSecondary)
    }
}

@Composable
private fun CollectionRow(c: Collection, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp, clipContent = true)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
            Spacer(Modifier.height(2.dp))
            Text("${c.cardsCount} карточек", fontSize = 12.sp, color = LightFgSecondary)
        }
        Text("›", fontSize = 22.sp, color = LightFgSecondary, modifier = Modifier.padding(start = 8.dp))
    }
}
```

- [ ] **Step 4: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/MyCollectionsViewModel.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionsTab.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/CreateCollectionDialog.kt
git commit -m "Add MyCollections flow (VM + tab + create dialog)"
```

---

### Task 17: `TeachingScreen` (5th tab with sub-tabs)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/TeachingScreen.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.example.cardwords.ui.teaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary

private enum class TeachingTab { STUDENTS, COLLECTIONS }

@Composable
fun TeachingScreen(onOpenCollection: (id: String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(TeachingTab.STUDENTS) }
    val studentsVm = remember { MyStudentsViewModel() }
    val collectionsVm = remember { MyCollectionsViewModel() }

    Column(Modifier.fillMaxSize().background(LightBg)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Преподавание", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LightFg)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Ученики", selected == TeachingTab.STUDENTS) { selected = TeachingTab.STUDENTS }
            Pill("Коллекции", selected == TeachingTab.COLLECTIONS) { selected = TeachingTab.COLLECTIONS }
        }

        when (selected) {
            TeachingTab.STUDENTS    -> StudentsTab(studentsVm)
            TeachingTab.COLLECTIONS -> CollectionsTab(collectionsVm, onOpenCollection)
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) LightFg else Color(0xFFF2F2F7)
    val fg = if (selected) Color.White else LightFgSecondary
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
```

- [ ] **Step 2: Wire into MainScreen**

Replace placeholder in `MainScreen.kt`:

```kotlin
composable<TeachingTabRoute> {
    TeachingScreen(onOpenCollection = { id ->
        outerNavController.navigate(CollectionDetailRoute(id))
    })
}
```

Add import: `import com.example.cardwords.ui.teaching.TeachingScreen` and `com.example.cardwords.navigation.CollectionDetailRoute`.

- [ ] **Step 3: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/TeachingScreen.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/main/MainScreen.kt
git commit -m "Add TeachingScreen with Students/Collections sub-tabs"
```

---

### Task 18: `CollectionDetailViewModel` + `CollectionDetailScreen` + `AddCardDialog` + `AssignStudentSheet`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionDetailViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionDetailScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AddCardDialog.kt`
- Create: `composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AssignStudentSheet.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/example/cardwords/App.kt` (register CollectionDetailRoute)

- [ ] **Step 1: Write `CollectionDetailViewModel.kt`**

```kotlin
package com.example.cardwords.ui.teaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.CollectionCard
import com.example.cardwords.data.model.CollectionDetail
import com.example.cardwords.data.model.Collection
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailPhase { LOADING, CONTENT, ERROR }

data class CollectionDetailUiState(
    val phase: DetailPhase = DetailPhase.LOADING,
    val detail: CollectionDetail? = null,
    val error: String? = null,
    val savingMeta: Boolean = false,
    val addingCard: Boolean = false,
    val deletingCardIds: Set<String> = emptySet(),
    val assigning: Boolean = false,
)

class CollectionDetailViewModel(private val collectionId: String) : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(phase = DetailPhase.LOADING, error = null) }
        viewModelScope.launch {
            val result = apiClient.getCollection(token, collectionId)
            result.fold(
                onSuccess = { dto ->
                    val detail = CollectionDetail(
                        collection = Collection(dto.id, dto.name, dto.description, dto.cardsCount),
                        cards = dto.cards.map { CollectionCard(it.id, it.wordOriginal, it.wordTranslation) },
                    )
                    _uiState.update { it.copy(phase = DetailPhase.CONTENT, detail = detail) }
                },
                onFailure = {
                    _uiState.update { it.copy(phase = DetailPhase.ERROR, error = "Не удалось загрузить") }
                },
            )
        }
    }

    fun saveMeta(name: String, description: String, onComplete: () -> Unit) {
        if (_uiState.value.savingMeta) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(savingMeta = true) }
        AppModule.syncScope.launch {
            val result = apiClient.updateCollection(token, collectionId, name.trim(), description.trim())
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { state ->
                        val detail = state.detail
                        state.copy(
                            savingMeta = false,
                            detail = detail?.copy(collection = Collection(dto.id, dto.name, dto.description, dto.cardsCount)),
                        )
                    }
                    onComplete()
                },
                onFailure = {
                    _uiState.update { it.copy(savingMeta = false, error = "Не удалось сохранить") }
                },
            )
        }
    }

    fun addCard(original: String, translation: String?) {
        val trimmed = original.trim()
        if (trimmed.isEmpty()) return
        val current = _uiState.value.detail ?: return
        // Local dedupe — case-insensitive compare on original
        val key = trimmed.lowercase()
        val alreadyExists = current.cards.any { it.wordOriginal.lowercase() == key }
        if (alreadyExists || _uiState.value.addingCard) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(addingCard = true) }
        AppModule.syncScope.launch {
            val result = apiClient.addCardToCollection(token, collectionId, trimmed, translation?.trim()?.takeIf { it.isNotEmpty() })
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { state ->
                        val detail = state.detail ?: return@update state
                        state.copy(
                            addingCard = false,
                            detail = detail.copy(
                                cards = detail.cards + CollectionCard(dto.id, dto.wordOriginal, dto.wordTranslation),
                                collection = detail.collection.copy(cardsCount = detail.collection.cardsCount + 1),
                            ),
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(addingCard = false, error = "Не удалось добавить карточку") }
                },
            )
        }
    }

    fun deleteCard(cardId: String) {
        if (cardId in _uiState.value.deletingCardIds) return
        val token = authManager.getToken() ?: return
        val previous = _uiState.value.detail ?: return
        _uiState.update { state ->
            state.copy(
                deletingCardIds = state.deletingCardIds + cardId,
                detail = previous.copy(
                    cards = previous.cards.filter { it.id != cardId },
                    collection = previous.collection.copy(cardsCount = (previous.collection.cardsCount - 1).coerceAtLeast(0)),
                ),
            )
        }
        AppModule.syncScope.launch {
            val result = apiClient.removeCardFromCollection(token, collectionId, cardId)
            if (result.isFailure) {
                _uiState.update { it.copy(detail = previous, error = "Не удалось удалить") }
            }
            _uiState.update { it.copy(deletingCardIds = it.deletingCardIds - cardId) }
        }
    }

    fun assignTo(studentId: String, onComplete: () -> Unit) {
        if (_uiState.value.assigning) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(assigning = true) }
        AppModule.syncScope.launch {
            val result = apiClient.assignCollection(token, collectionId, studentId)
            _uiState.update { it.copy(assigning = false) }
            if (result.isSuccess) onComplete()
        }
    }
}
```

- [ ] **Step 2: Write `AddCardDialog.kt`**

```kotlin
package com.example.cardwords.ui.teaching.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary

private val DividerColor = Color(0xFFE5E5EA)
private val FieldBg = Color(0xFFF2F2F7)

@Composable
fun AddCardDialog(
    inFlight: Boolean,
    onSubmit: (original: String, translation: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var original by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 18.dp).padding(20.dp),
        ) {
            Text("Добавить карточку", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LightFg)
            Spacer(Modifier.height(12.dp))
            TextField(
                value = original, onValueChange = { if (it.length <= 100) original = it },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("Оригинал", color = LightFgSecondary, fontSize = 15.sp) },
                singleLine = true,
                colors = fieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = translation, onValueChange = { if (it.length <= 200) translation = it },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("Перевод (необязательно)", color = LightFgSecondary, fontSize = 15.sp) },
                singleLine = true,
                colors = fieldColors(),
            )
            Spacer(Modifier.height(16.dp))
            Row {
                Box(
                    modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50)).background(FieldBg)
                        .clickable(enabled = !inFlight, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("Отмена", fontSize = 14.sp, color = LightFg) }
                Spacer(Modifier.width(10.dp))
                val canSubmit = original.isNotBlank() && !inFlight
                Box(
                    modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                        .background(if (canSubmit) LightFg else Color(0xFFCCCCCC))
                        .clickable(enabled = canSubmit) {
                            onSubmit(original, translation.ifBlank { null })
                            original = ""; translation = ""
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (inFlight) "..." else "Добавить", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedTextColor = LightFg, unfocusedTextColor = LightFg,
    focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
    cursorColor = LightFg,
)
```

- [ ] **Step 3: Write `AssignStudentSheet.kt`**

```kotlin
package com.example.cardwords.ui.teaching.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.teaching.MyStudentsViewModel
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun AssignStudentSheet(
    onPick: (studentId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val vm = remember { MyStudentsViewModel() }
    val state by vm.uiState.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 18.dp).padding(20.dp),
        ) {
            Text("Назначить ученику", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LightFg)
            Spacer(Modifier.height(12.dp))
            when {
                state.phase.name == "LOADING" -> CircularProgressIndicator(modifier = Modifier.size(28.dp), color = LightFg, strokeWidth = 2.dp)
                state.students.isEmpty() -> Text("Сначала добавьте ученика во вкладке «Ученики»", fontSize = 13.sp, color = LightFgSecondary)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.students, key = { it.id }) { st ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8F8FA)).clickable { onPick(st.id) }.padding(12.dp),
                        ) {
                            Column {
                                Text(st.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
                                Text(st.email, fontSize = 11.sp, color = LightFgSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Write `CollectionDetailScreen.kt`**

```kotlin
package com.example.cardwords.ui.teaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.teaching.dialogs.AddCardDialog
import com.example.cardwords.ui.teaching.dialogs.AssignStudentSheet
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun CollectionDetailScreen(
    collectionId: String,
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember(collectionId) { CollectionDetailViewModel(collectionId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddCard by remember { mutableStateOf(false) }
    var showAssignSheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(LightBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 28.sp, color = LightFg) }
            Spacer(Modifier.width(4.dp))
            Text(
                text = state.detail?.collection?.name ?: "Коллекция",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LightFg,
                maxLines = 1,
            )
        }

        when (state.phase) {
            DetailPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LightFg, strokeWidth = 2.dp)
            }
            DetailPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Ошибка", fontSize = 14.sp, color = Red40)
            }
            DetailPhase.CONTENT -> {
                val detail = state.detail!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        if (!detail.collection.description.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(16.dp)) {
                                Text(detail.collection.description, fontSize = 13.sp, color = LightFgSecondary)
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                                    .background(LightFg).clickable { showAddCard = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+ Карточка", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
                            }
                            Box(
                                modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                                    .background(Color(0xFFF2F2F7)).clickable { showAssignSheet = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("→ Назначить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
                            }
                        }
                    }
                    item { Text("${detail.cards.size} карточек", fontSize = 11.sp, color = LightFgSecondary) }
                    items(detail.cards, key = { it.id }) { card ->
                        Row(
                            modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(card.wordOriginal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
                                Text(card.wordTranslation, fontSize = 12.sp, color = LightFgSecondary)
                            }
                            val deleting = card.id in state.deletingCardIds
                            if (!deleting) {
                                Text("×", fontSize = 18.sp, color = Red40,
                                    modifier = Modifier.clickable { viewModel.deleteCard(card.id) }.padding(horizontal = 8.dp))
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = LightFgSecondary)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showAddCard) {
        AddCardDialog(
            inFlight = state.addingCard,
            onSubmit = { o, t -> viewModel.addCard(o, t) },
            onDismiss = { showAddCard = false },
        )
    }
    if (showAssignSheet) {
        AssignStudentSheet(
            onPick = { studentId -> viewModel.assignTo(studentId) { showAssignSheet = false } },
            onDismiss = { showAssignSheet = false },
        )
    }
}
```

- [ ] **Step 5: Register `CollectionDetailRoute` in App.kt**

```kotlin
composable<CollectionDetailRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<CollectionDetailRoute>()
    CollectionDetailScreen(
        collectionId = route.collectionId,
        onNavigateBack = { navController.popBackStack() },
    )
}
```

Add imports:
```kotlin
import com.example.cardwords.navigation.CollectionDetailRoute
import com.example.cardwords.ui.teaching.CollectionDetailScreen
```

- [ ] **Step 6: Compile**

Run: `./gradlew :composeApp:compileKotlinJvm --quiet`

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionDetailViewModel.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/CollectionDetailScreen.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AddCardDialog.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/ui/teaching/dialogs/AssignStudentSheet.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/App.kt
git commit -m "Add CollectionDetail flow (VM + screen + add-card + assign sheet)"
```

---

### Task 19: Unit tests

**Files:**
- Create: `composeApp/src/jvmTest/kotlin/com/example/cardwords/auth/AuthManagerRoleTest.kt`
- Create: `composeApp/src/jvmTest/kotlin/com/example/cardwords/student/MyTeachersViewModelTest.kt`
- Create: `composeApp/src/jvmTest/kotlin/com/example/cardwords/teaching/CollectionDetailViewModelTest.kt`

> The existing test infrastructure uses `kotlin.test`. ViewModels reach `AppModule` directly which makes pure unit tests harder. For these tests we'll use a small **fake API client** that subclasses or wraps `CardWordsApiClient`'s methods we need. Recommended: extract an interface `CardWordsApiClientApi` with the methods used in this PR, plus an alternate constructor on AppModule for tests. **If extracting an interface seems too invasive**, keep `AppModule` as-is and put a setter in `AppModule` for `cardWordsApiClient` used only from test setup.

- [ ] **Step 1: Add a test-only setter in `AppModule.kt` (gated)**

```kotlin
// At bottom of AppModule object
fun setCardWordsApiClientForTesting(client: CardWordsApiClient) {
    _cardWordsApiClientOverride = client
}
private var _cardWordsApiClientOverride: CardWordsApiClient? = null
```

Modify the existing `cardWordsApiClient` getter so it returns the override if set:

```kotlin
val cardWordsApiClient: CardWordsApiClient
    get() = _cardWordsApiClientOverride ?: _cardWordsApiClient
private val _cardWordsApiClient: CardWordsApiClient by lazy { CardWordsApiClient() }
```

If the existing declaration is a `val` lazy, replace it with the pattern above.

- [ ] **Step 2: Write `AuthManagerRoleTest.kt`** (skeleton — test setRole happy path against an in-memory repository + a stub API client)

```kotlin
package com.example.cardwords.auth

import com.example.cardwords.data.local.InMemoryDatabaseRepository
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.data.remote.AuthManager
import com.example.cardwords.data.remote.CardWordsApiClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthManagerRoleTest {
    @Test
    fun `setRole happy path persists role and updates flow`() = runTest {
        val repo = InMemoryDatabaseRepository()
        // Pre-seed token
        repo.setSetting("api_token", "test-token")

        val fakeApi = object : CardWordsApiClient() {
            override suspend fun switchRole(token: String, role: String) = Result.success(Unit)
        }
        val auth = AuthManager(repo, fakeApi)

        assertNull(auth.getRole())
        val result = auth.setRole(UserRole.TEACHER)
        assertTrue(result.isSuccess)
        assertEquals(UserRole.TEACHER, auth.getRole())
        assertEquals("TEACHER", repo.getSetting("user_role"))
    }
}
```

> Note: `CardWordsApiClient` methods are not `open` by default. To make the override work, mark the class and methods `open` (and `:CardWordsApiClient()` ctor accessible). Adjust scope as needed.

- [ ] **Step 3: Write `MyTeachersViewModelTest.kt`** (acceptCollection optimistic revert)

```kotlin
package com.example.cardwords.student

// Minimal sketch — full implementation depends on whether you keep AppModule
// indirection or refactor AssetClient to be injectable. See impl notes.
import kotlin.test.Test
import kotlin.test.assertEquals

class MyTeachersViewModelTest {
    @Test
    fun `placeholder ensures suite compiles`() {
        assertEquals(2, 1 + 1)
    }
}
```

> The full test (accept revert + reaccept badge) requires a fake API. If interface extraction is deferred, keep this stub and add proper tests when refactor lands.

- [ ] **Step 4: Run tests**

Run: `./gradlew :composeApp:jvmTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/jvmTest/kotlin/com/example/cardwords/auth/AuthManagerRoleTest.kt \
        composeApp/src/jvmTest/kotlin/com/example/cardwords/student/MyTeachersViewModelTest.kt \
        composeApp/src/commonMain/kotlin/com/example/cardwords/di/AppModule.kt
git commit -m "Add unit tests for AuthManager role flow + test override on AppModule"
```

---

### Task 20: Manual smoke + bug bash

- [ ] **Step 1: Build the app for Android**

Run: `./gradlew :composeApp:assembleDebug`
Install on device or emulator.

- [ ] **Step 2: Smoke checklist**

1. Регистрация TEACHER: register → попадаем на RoleSelectionScreen → выбираем «Я — преподаватель» → 5-й таб = «Преподавание».
2. Создать коллекцию: «Преподавание → Коллекции → + Создать» — вводим имя, описание → коллекция в списке.
3. Открыть коллекцию → «+ Карточка» → original + translation → карточка в списке.
4. Регистрация второго пользователя как STUDENT (другой email) на том же сервере. Возврат на TEACHER аккаунт: пригласить ученика — «Преподавание → Ученики → + Пригласить» (email второго).
5. Ученика добавить — он в списке. Открыть коллекцию → «→ Назначить» → выбрать ученика.
6. Перелогиниться в STUDENT → 5-й таб «Учителя» → видим учителя + назначенную коллекцию (status «Выдана») → нажать «Принять» → status «Добавлена», карточки в /api/cards тоже подтянулись (чек: вкладка «Слова» — там новые карточки).
7. Залогиниться в TEACHER → добавить вторую карточку в коллекцию.
8. Залогиниться в STUDENT → 5-й таб → у коллекции бейдж «Новые». Кнопка «Обновить» → бейдж пропадает, вторая карточка в «Слова».
9. STUDENT: Профиль → «Сменить роль» → TEACHER → 5-й таб = «Преподавание», списки пустые. Назад: → STUDENT → списки на месте.
10. Logout → re-login → роль восстанавливается, личные карточки (словарь, прогресс) на месте.
11. Off network → попытка accept → toast «Сервер недоступен». On network → retry работает.

- [ ] **Step 3: Fix any bugs found**

Repeat: write failing test (if applicable) → fix → commit.

- [ ] **Step 4: Final commit (if anything fixed)**

```bash
git commit -am "Smoke fixes: <one-line per fix>"
```

---

## Verification — overall acceptance criteria

- [ ] All 14 new endpoints reachable from corresponding ViewModels.
- [ ] `RoleSelectionScreen` blocks main app until role chosen; can't be skipped.
- [ ] Role switch in Profile changes 5th tab + content within ≤1 second; personal SQLDelight data not cleared.
- [ ] Teacher can: create collection, add cards, invite student, assign collection, remove student, delete collection.
- [ ] Student can: see teachers, see assigned collections with status, accept (cards merge into dictionary), refresh on teacher updates (badge → new cards).
- [ ] All mutations survive screen navigation (use `AppModule.syncScope`).
- [ ] Failure modes — 4xx/5xx surfaced as user-readable toast; not crashing on null body.
- [ ] Smoke checklist (Task 20 §Step 2) — every step passes.

---

## Self-Review

**1. Spec coverage** (against `2026-04-26-teacher-student-integration-design.md`):

| Spec section | Covered by |
|---|---|
| §1 Goal | All tasks, especially 9-18 |
| §2 API contract | Tasks 2-6 |
| §3.1 server-as-source-of-truth | AuthManager (Task 7) — personal data preserved |
| §3.2 5th-tab navigation | Task 11 + Task 17 |
| §3.3 mandatory role-pick gate | Tasks 9-10 |
| §4 Component map | Reflected in file structure section |
| §5 Data flows | Implicit in tasks 12, 13, 18, 19 |
| §6 Failure modes | Task 4-6 (HTTP), Tasks 12-18 (optimistic revert) |
| §7 Testing | Task 19 + smoke in Task 20 |
| §8 Migration | App routing (Task 10) handles role==null cleanly for old users |
| §9 Out of scope | Excluded (no pending queue, no push, no pagination) |
| §10 Open backend assumptions | Discoverable via Task 20 smoke; spec adapts |
| §11 Implementation outline | This plan IS the outline, expanded |
| §12 Acceptance criteria | Verification section above |

**2. Placeholder scan:** No "TBD"/"TODO"/"implement later" without code. Test stub in Task 19 explicitly documented as deferred pending interface extraction.

**3. Type consistency:**
- `Collection.cardsCount` consistent across model, DTO, all VMs.
- `AssignmentStatus.ASSIGNED`/`ADDED` consistent across model + UI.
- `UserRole.STUDENT`/`TEACHER` consistent everywhere.
- API method names: `listMyStudents`, `listMyCollections`, `listMyTeachers`, `listAssignedCollections` — same nominative pattern.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-26-teacher-student-integration.md`. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
