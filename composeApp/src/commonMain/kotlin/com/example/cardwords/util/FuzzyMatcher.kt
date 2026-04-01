package com.example.cardwords.util

object FuzzyMatcher {

    fun isCloseEnough(input: String, expected: String): Boolean {
        val normalizedInput = input.trim().lowercase()
        val normalizedExpected = expected.trim().lowercase()
        if (normalizedInput == normalizedExpected) return true
        if (normalizedInput.length < normalizedExpected.length - 1) return false
        val maxDistance = maxOf(1, normalizedExpected.length / 5)
        return levenshteinDistance(normalizedInput, normalizedExpected) <= maxDistance
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost,
                )
            }
        }

        return dp[m][n]
    }
}
