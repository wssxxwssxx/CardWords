package com.example.cardwords.student

// Minimal sketch — full implementation depends on whether AppModule is refactored
// behind an interface (or AssetClient becomes injectable). Today MyTeachersViewModel
// reaches AppModule directly, which couples instantiation to a global initialised
// state and makes pure unit tests impractical without a wider refactor.
//
// See the parent plan (Task 19, "When You're in Over Your Head" → DOWNSCOPE notes)
// for context. AppModule.setCardWordsApiClientForTesting was added in this commit
// as the first step toward enabling these tests; the next step (out of scope here)
// is making the repository injectable too.
import kotlin.test.Test
import kotlin.test.assertEquals

class MyTeachersViewModelTest {

    @Test
    fun placeholder_ensuresSuiteCompiles() {
        assertEquals(2, 1 + 1)
    }
}
