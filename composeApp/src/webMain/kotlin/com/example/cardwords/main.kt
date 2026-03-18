package com.example.cardwords

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.cardwords.data.local.InMemoryDatabaseRepository
import com.example.cardwords.di.AppModule

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    AppModule.initialize(InMemoryDatabaseRepository())
    ComposeViewport {
        App()
    }
}
