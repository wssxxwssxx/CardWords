package com.example.cardwords

import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.cardwords.data.local.SqlDelightDatabaseRepository
import com.example.cardwords.db.CardWordsDatabase
import com.example.cardwords.di.AppModule
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

fun MainViewController() = run {
    val driver = NativeSqliteDriver(CardWordsDatabase.Schema, "cardwords.db")
    AppModule.initialize(SqlDelightDatabaseRepository(driver) {
        (NSDate().timeIntervalSince1970 * 1000).toLong()
    })
    ComposeUIViewController { App() }
}
