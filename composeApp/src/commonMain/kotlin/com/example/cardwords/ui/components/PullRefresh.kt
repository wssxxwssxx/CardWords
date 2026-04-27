package com.example.cardwords.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Reusable pull-to-refresh wrapper. Shows spinner while [onRefresh] is in progress.
 * onRefresh should trigger any data reload; isRefreshing auto-resets after [minSpinMs]
 * to give visual feedback even for instant operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefresh(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    minSpinMs: Long = 600L,
    content: @Composable () -> Unit,
) {
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(minSpinMs)
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        },
        modifier = modifier,
    ) {
        content()
    }
}
