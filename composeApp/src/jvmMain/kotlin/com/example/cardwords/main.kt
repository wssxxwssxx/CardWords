package com.example.cardwords

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.cardwords.data.local.SqlDelightDatabaseRepository
import com.example.cardwords.db.CardWordsDatabase
import com.example.cardwords.di.AppModule
import java.io.File

fun main() {
    val dbFile = File("cardwords.db")
    val dbExists = dbFile.exists()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    if (!dbExists) {
        CardWordsDatabase.Schema.create(driver)
    }
    AppModule.initialize(SqlDelightDatabaseRepository(driver) { System.currentTimeMillis() })

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "CardWords",
        ) {
            App()
        }
    }
}
