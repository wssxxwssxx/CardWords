# Teacher / Student интеграция в Android-клиент

**Status:** Design draft (awaiting user approval)
**Author:** wssxx (with Claude)
**Date:** 2026-04-26

## 1. Цель

Подключить уже существующую на сервере `http://64.188.60.84` фичу **«учитель ↔ ученик ↔ коллекции»** в мобильный клиент CardWords (Compose Multiplatform). Поведение в полном объёме (без deferred features, кроме оговоренных в §10):

- Регистрация без role; обязательный выбор роли после создания аккаунта.
- Гибкая смена роли в любой момент через `PUT /api/auth/role`.
- **Учитель:** управление учениками (invite по email / remove), коллекциями (CRUD), карточками внутри коллекции (add / remove), назначениями (assign коллекции ученикам).
- **Ученик:** список своих учителей; список назначенных коллекций со статусами «Выдана / Добавлена»; принятие коллекций (карточки попадают в личный словарь); badge «Новые карточки» при обновлении принятой коллекции.

## 2. API-контракт (источник: JS-bundle SPA на `64.188.60.84/assets/index-DniGbe58.js`)

Все пути идут под префиксом `/api`.

### 2.1 Auth (изменения)

| Метод | URL | Body | Резюме |
|---|---|---|---|
| `POST` | `/api/auth/register` | `{email, name, password}` | role **не** передаётся; пользователь создаётся без роли |
| `PUT`  | `/api/auth/role` | `{role: "STUDENT" \| "TEACHER"}` | устанавливает / меняет роль |
| `GET`  | `/api/auth/me` | — | возвращает `{..., role: "STUDENT" \| "TEACHER" \| null}` |

### 2.2 Teacher endpoints

| Метод | URL | Body | Назначение |
|---|---|---|---|
| `GET`    | `/api/teacher/students` | — | список учеников |
| `POST`   | `/api/teacher/students` | `{email}` | пригласить ученика |
| `DELETE` | `/api/teacher/students/{id}` | — | удалить ученика |
| `GET`    | `/api/teacher/collections` | — | список коллекций |
| `POST`   | `/api/teacher/collections` | `{name, description}` | создать коллекцию |
| `GET`    | `/api/teacher/collections/{id}` | — | детали коллекции (с карточками) |
| `PUT`    | `/api/teacher/collections/{id}` | `{name, description}` | обновить |
| `DELETE` | `/api/teacher/collections/{id}` | — | удалить |
| `POST`   | `/api/teacher/collections/{cid}/cards` | `{word_original, word_translation}` | добавить карточку |
| `DELETE` | `/api/teacher/collections/{cid}/cards/{cardId}` | — | удалить карточку |
| `POST`   | `/api/teacher/collections/{cid}/assign/{studentId}` | — | назначить коллекцию ученику |

### 2.3 Student endpoints

| Метод | URL | Назначение |
|---|---|---|
| `GET`  | `/api/student/teachers` | список своих учителей |
| `GET`  | `/api/student/collections` | список назначенных коллекций со статусом |
| `POST` | `/api/student/collections/{id}/add` | принять коллекцию (cards → личный словарь) |

### 2.4 JSON shapes (по UI-связкам в bundle)

```jsonc
// Collection (teacher list)
{ "id": "...", "name": "...", "description": "...", "cards_count": 10 }

// Collection (teacher detail) — добавляется список cards
{ "id", "name", "description", "cards_count", "cards": [{"id", "word_original", "word_translation"}, ...] }

// AssignedCollection (student side)
{ "id", "name", "description", "cards_count": 10, "teacher_name": "...", "status": "assigned" | "added" }

// StudentSummary
{ "id", "email", "name", "cards_count": 50 }

// TeacherSummary
{ "id", "email", "name" }
```

Точный набор полей не подтверждён через authenticated-запрос (не было токена); поля выше — те, что точно используются UI-компонентами в bundle. Любые дополнительные поля в JSON будут проигнорированы благодаря `ignoreUnknownKeys = true` в `Json` конфиге клиента.

## 3. Архитектурные решения

### 3.1 Гибкая роль, server — источник правды (вариант B)

`AuthManager.roleFlow: StateFlow<UserRole?>` подписывается UI-слой. Личные данные (`word`, `word_progress`, `study_session`, `daily_activity`, `achievement`, `user_settings`) не зависят от роли — никогда не очищаются на role switch. Role-scoped state (списки учеников / коллекций / учителей) — только in-memory кэш в ViewModel'ах, тянется с сервера on demand.

### 3.2 Навигация — пятый «контекстный» таб (вариант 3)

Существующие 4 таба сохраняются. Добавляется пятый, лейбл/контент зависит от `roleFlow`:

```
STUDENT: Главная / Слова / Статистика / Учителя       / Профиль
TEACHER: Главная / Слова / Статистика / Преподавание  / Профиль
```

5-й таб у учителя — composable с двумя sub-tabs «Ученики» и «Коллекции».

При смене роли — `MainScreen` пересоздаёт `NavController` через `key(role) { rememberNavController() }`, чтобы старый back-stack и destinations не утекали.

### 3.3 Mandatory role-pick gate

`App.kt` маршрутизация после auth:

```
if (!loggedIn)            → WelcomeRoute
else if (role == null)    → RoleSelectionRoute   (нельзя пропустить)
else if (!onboardingDone) → OnboardingRoute
else                      → MainRoute
```

## 4. Component / file map

### 4.1 Новые domain models — `data/model/`

```kotlin
@Immutable enum class UserRole { STUDENT, TEACHER }

@Immutable data class Collection(
    val id: String, val name: String, val description: String?, val cardsCount: Int,
)
@Immutable data class CollectionDetail(
    val collection: Collection, val cards: List<CollectionCard>,
)
@Immutable data class CollectionCard(
    val id: String, val wordOriginal: String, val wordTranslation: String,
)

@Immutable data class StudentSummary(
    val id: String, val email: String, val name: String, val cardsCount: Int,
)
@Immutable data class TeacherSummary(val id: String, val email: String, val name: String)

@Immutable data class AssignedCollection(
    val collection: Collection, val teacherName: String, val status: AssignmentStatus,
    val hasNewCards: Boolean,  // вычисляется на клиенте — см. §6
)
enum class AssignmentStatus { ASSIGNED, ADDED }
```

### 4.2 `CardWordsApiClient` — добавляется 14 методов

Каждый явно проверяет `response.status.value` (паттерн PR #1) и возвращает `Result<T>`. Один общий helper `apiCall<T>(...)` или extension чтобы не дублировать boilerplate.

```kotlin
suspend fun switchRole(token: String, role: UserRole): Result<Unit>
suspend fun listMyStudents(token: String): Result<List<StudentSummary>>
suspend fun inviteStudent(token: String, email: String): Result<StudentSummary>
suspend fun removeStudent(token: String, studentId: String): Result<Unit>
suspend fun listMyCollections(token: String): Result<List<Collection>>
suspend fun createCollection(token: String, name: String, description: String): Result<Collection>
suspend fun getCollection(token: String, id: String): Result<CollectionDetail>
suspend fun updateCollection(token: String, id: String, name: String, description: String): Result<Collection>
suspend fun deleteCollection(token: String, id: String): Result<Unit>
suspend fun addCardToCollection(token: String, cid: String, original: String, translation: String?): Result<CollectionCard>
suspend fun removeCardFromCollection(token: String, cid: String, cardId: String): Result<Unit>
suspend fun assignCollection(token: String, cid: String, studentId: String): Result<Unit>
suspend fun listMyTeachers(token: String): Result<List<TeacherSummary>>
suspend fun listAssignedCollections(token: String): Result<List<AssignedCollection>>
suspend fun acceptCollection(token: String, cid: String): Result<Unit>
```

DTO для request/response — отдельные `@Serializable` классы в `data/remote/CardWordsApiModels.kt`.

### 4.3 `AuthManager` — расширения

```kotlin
val roleFlow: StateFlow<UserRole?>
fun getRole(): UserRole? = roleFlow.value
suspend fun setRole(role: UserRole): Result<Unit>   // PUT /auth/role + persist
suspend fun refreshRoleFromServer()                  // GET /auth/me — sync роли
```

`roleFlow` сохраняется в SQLDelight `user_settings` (key `"user_role"`) для быстрого старта, обновляется при `saveSession` и при `refreshRoleFromServer`.

### 4.4 ViewModels — новые, в `ui/teaching/` и `ui/student/`

```
RoleSelectionViewModel      — single-shot pick, после регистрации
MyTeachersViewModel         — student-side: teachers + assigned collections + accept
MyStudentsViewModel         — teacher-side: students + invite + remove
MyCollectionsViewModel      — teacher-side: collections list + create
CollectionDetailViewModel   — teacher-side push: edit / cards / assign
```

Все используют `viewModelScope` для load/observe, **`AppModule.syncScope`** для всех мутаций (паттерн PR #1), чтобы навигация не отменяла in-flight POST/PUT/DELETE.

### 4.5 Screens — новые

```
ui/auth/RoleSelectionScreen.kt          — gate после register; mandatory
ui/teaching/TeachingScreen.kt           — 5-й таб для teacher; sub-tabs Students / Collections
ui/teaching/StudentsTab.kt              — список + AddStudentDialog
ui/teaching/CollectionsTab.kt           — список + CreateCollectionDialog
ui/teaching/CollectionDetailScreen.kt   — push: name/desc edit + cards CRUD + AssignStudentSheet
ui/teaching/AddStudentDialog.kt         — email validation
ui/teaching/CreateCollectionDialog.kt   — name + description
ui/teaching/AssignStudentSheet.kt       — bottom sheet, выбор ученика для assign
ui/teaching/AddCardDialog.kt            — пара original / translation
ui/student/MyTeachersScreen.kt          — 5-й таб для student; teachers + assigned collections
```

Реюзают существующие design tokens (`LightBg`, `LightFg`, etc.), модификатор `cardBg()`, обёртку `PullRefresh`, Canvas-иконки в стиле `MainScreen.NavIconPaths`.

### 4.6 Routes — `navigation/Routes.kt`

```kotlin
@Serializable data object RoleSelectionRoute
@Serializable data class CollectionDetailRoute(val id: String)
```

### 4.7 `MainScreen` — модификация

```kotlin
val role by AppModule.authManager.roleFlow.collectAsState()
key(role) {                                    // пересоздание NavController при смене роли
    val tabNavController = rememberNavController()
    val tabs = remember(role) {
        when (role) {
            UserRole.STUDENT -> studentTabs
            UserRole.TEACHER -> teacherTabs
            null             -> emptyList()    // не должно случиться — gate выше
        }
    }
    Scaffold(bottomBar = { /* render tabs */ }) { padding ->
        NavHost(tabNavController, /* ... */) { /* ... */ }
    }
}
```

Где `studentTabs` / `teacherTabs` — top-level `val`-ы со списком `TabItem` (как существующий `tabs` в `MainScreen`).

## 5. Data flow ключевых сценариев (резюме)

### 5.1 Cold start

`/auth/me` тянем при старте, обновляем `roleFlow`. Маршрутизация в `App.kt` — см. §3.3.

### 5.2 Role switch (Профиль → «Сменить роль»)

`AuthManager.setRole(newRole)` → `PUT /auth/role` → on success `roleFlow.emit(newRole)` → `MainScreen` пересоздаёт NavController → tabs пересобираются. Personal data в SQLDelight не трогается. Role-кнопка disabled пока активны диалоги.

### 5.3 Teacher: создать → добавить карточку → назначить

```
POST /teacher/collections {name, description}                       — fire-and-forget на syncScope
POST /teacher/collections/{cid}/cards {word_original, word_translation}
POST /teacher/collections/{cid}/assign/{studentId}                  — 409 трактуется как success
```

Optimistic UI; rollback при failure; assign идемпотентен.

### 5.4 Student: принять коллекцию

```
POST /student/collections/{id}/add
on success:
    repository.syncCardsFromServer()        — pulls /api/cards и мёрджит в SQLDelight
                                              (вызов не зависит от того, на каком экране пользователь)
    user_settings["accepted_coll_<id>"] = collection.cardsCount
```

`syncCardsFromServer` — общий метод (новый или вынесенный из существующей логики `DictionaryViewModel.refresh`), доступный из любого VM. Так после accept ученик увидит новые карточки в любом табе, не только на «Слова».

### 5.5 Re-accept badge

При каждом load `/student/collections`: для каждого item со status=ADDED сравниваем `cardsCount` с локально сохранённым `accepted_coll_<id>`. Если `>`, ставим `hasNewCards=true`. Badge «Новые карточки» в UI; клик «Обновить» вызывает тот же `acceptCollection` (server идемпотентен — добавляет только новое); после success обновляется локальный счётчик.

## 6. Failure modes & robustness

### 6.1 HTTP

| Status | Реакция |
|---|---|
| 200/201/204 | success |
| 401 | `AuthManager.logout()` + nav `WelcomeRoute` |
| 403 | refetch `/auth/me`, `roleFlow` обновляется, UI пересоберётся |
| 404 на ресурс | toast «Удалено или недоступно» + refresh списка |
| 409 (idempotent ops) | trace as success (assign, accept) |
| 409 (unique-conflict) | error toast (createCollection с занятым именем) |
| 422 | парсим `{detail: [{loc, msg}, ...]}` от FastAPI, показываем human-readable |
| 429 | toast «Подождите» + кнопка disabled на 5 сек |
| 5xx / network | toast «Сервер недоступен» + retry button; локальное оптимистичное состояние сохраняется |

### 6.2 Race conditions

- **Double-tap mutations** — каждый VM хранит `Set<String>` of in-flight ids; кнопка disabled пока id в сете.
- **Role switch с открытым диалогом** — кнопка role disabled пока активны диалоги.
- **Cross-device drift** — рефреш `/auth/me` на `Lifecycle.Event.ON_RESUME` + 403-fallback.

### 6.3 Optimistic update rules

1. Snapshot предыдущего state.
2. Apply optimistic.
3. Server call.
4. Success → ничего (оставляем optimistic).
5. Failure → restore snapshot + toast.

После некоторых успехов нужен **дополнительный refetch** (например `acceptCollection` → `/cards`), но не «двойная отрисовка» — только когда сервер действительно изменил что-то ещё.

### 6.4 Offline

Teacher/student feature **online-only** в v1 (нет pending queue). Поведение:

- **Cold load 5-го таба без сети** (нет in-memory кэша) → empty state «Нет соединения» + retry button.
- **Warm cache + потеря сети** (юзер уже листал список, потом сеть пропала) → показываем закешированное содержимое + sticky warning bar «Нет соединения», retry-кнопкой; никакого внезапного обнуления списка.
- Кнопка «Сменить роль» в Профиле disabled при отсутствии сети.
- Любая мутация (invite, create, assign, accept, etc.) — disabled offline; кнопки возвращаются в активный статус по `connectivityFlow`.

YAGNI на v1. Pending queue — v2.

### 6.5 Validation (форма-side, до отправки)

| Поле | Правило |
|---|---|
| email (invite) | regex `[^@]+@[^@]+\.[^@]+`, не пустой |
| name (collection) | trimmed, не пустой, ≤ 100 chars |
| description (collection) | optional, ≤ 500 chars |
| word_original | trimmed, не пустой, ≤ 100 chars |
| word_translation | optional, ≤ 200 chars |

Submit disabled пока валидация не пройдена; не дожидаемся 422.

### 6.6 Empty / loading / error / content states

Каждый новый screen и tab имеет 4 state'а явно (повторяет паттерн `DictionaryScreen` после редизайна).

## 7. Testing strategy

### 7.1 Unit (composeApp:jvmTest)

| Тест | Проверяет |
|---|---|
| `RoleSelectionViewModelTest` | setRole happy / failure → roleFlow |
| `MyTeachersViewModelTest::accept_optimistic_revert` | failure-path откат |
| `MyTeachersViewModelTest::reaccept_badge_logic` | `cardsCount > lastKnown` → hasNewCards |
| `CollectionDetailViewModelTest::deduplicate_addCard` | повтор original локально дедуплицируется до запроса |
| `AuthManagerTest::role_drift_on_403` | 403 → refreshRole вызвался |
| `CardWordsApiClientTest::http_status_propagation` | 4xx/5xx → Result.failure |

`InMemoryDatabaseRepository` уже есть — реюзаем для VM-тестов.

### 7.2 Manual smoke checklist

```
1. Register TEACHER → create collection → invite student → assign collection
2. Register second STUDENT (того email из шага 1) → видит teacher и assigned collection
   → accept → cards в словаре
3. Teacher add card → student видит badge «Новые карточки» → «Обновить» → cards в словаре
4. Switch role student → teacher → empty списки → switch back → personal data на месте
5. Logout → re-login → role восстанавливается
6. Network off → попытка accept → toast «Сервер недоступен» → network on → retry работает
```

### 7.3 Не покрываем в v1

- Compose UI snapshot tests
- E2E (Espresso)
- Network flakiness simulation (only manual через airplane-mode)

## 8. Migration plan для существующих пользователей

- Текущие пользователи в БД — без `role`. После update: `roleFlow.value = null` → mandatory `RoleSelectionScreen` при следующем launch.
- Personal data (словарь, прогресс) сохраняется.
- Token остаётся валидным — не нужно re-login.

## 9. Out of scope в v1 (вошло в follow-up)

- Pending sync queue для offline-режима в teacher/student функциях.
- Push-уведомления ученику о новой назначенной коллекции.
- Compose UI snapshot tests.
- Pagination для очень больших списков учеников / коллекций (>100). Пока fetch всех; если станет проблемой — backend pagination + LazyColumn pagination в v2.
- I18n; все строки на русском, как и в текущем приложении.
- Удаление принятой коллекции учеником из своего списка («забыть»). Сейчас одного направления — accept; revert не предусмотрен сервером.

## 10. Открытые предположения о backend-контракте

Эти моменты нужно подтвердить при первом authenticated-запросе или сэмпле response от пользователя:

1. `POST /api/auth/register` — возвращает 201 + UserResponse без role или 200 + token? Влияет на flow в `AuthViewModel`.
2. `GET /api/auth/me` — поле `role` в response: `string | null`?
3. `POST /api/teacher/collections/{cid}/assign/{studentId}` — что возвращает при повторном assign'е (200 идемпотент или 409)?
4. `POST /api/student/collections/{cid}/add` — идемпотентен ли? Если коллекция уже added и teacher добавил карточки, accept должен подмёрджить только новые.
5. Body of `addCardToCollection` — `word_translation` обязателен или сервер сам переводит при `null`?

В коде клиента предполагаем best-practice (идемпотентность, optional translation, 409 как success в идемпотентных ops). Если на сервере не так — backend нужно поправить или клиент адаптировать; этот пункт фиксируется как риск.

## 11. Implementation outline (для writing-plans)

Будет подробно расписано в плане; здесь скелет порядка задач:

1. Domain models + сериализация DTO + `UserRole` enum + утилиты
2. `CardWordsApiClient` extensions (14 методов) с общим helper'ом для status check
3. `AuthManager` — `roleFlow`, persist, refresh, switch
4. `RoleSelectionScreen` + VM + интеграция в `App.kt` routing gate
5. `MainScreen` — `key(role) { ... }`, dynamic tabs
6. `MyTeachersScreen` + VM (student-side) + accept flow + badge logic
7. `TeachingScreen` + sub-tabs + dialogs (teacher-side)
8. `CollectionDetailScreen` + VM + dialogs (cards CRUD, assign sheet)
9. Profile «Сменить роль» action + disable rules
10. Unit tests (Section 7.1)
11. Manual smoke pass (Section 7.2) + bug bash
12. Open questions (§10) валидируем — поправки в клиенте если расхождения

## 12. Acceptance criteria

- Все 14 endpoints вызываются, ответы корректно парсятся при `ignoreUnknownKeys`.
- Mandatory role-pick gate работает после регистрации.
- Гибкая смена роли в Профиле без перерегистрации; personal data сохраняется.
- Учитель: создать коллекцию, добавить карточки, пригласить ученика по email, назначить коллекцию, удалить ученика, удалить коллекцию.
- Ученик: видеть назначенные коллекции, принять, видеть badge «Новые карточки» при обновлении.
- Все мутации на `AppModule.syncScope` — survive screen navigation.
- Failure modes (§6) — все 4xx/5xx обработаны, не падает на null body.
- Manual smoke checklist (§7.2) — пройден.
