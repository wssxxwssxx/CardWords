package com.example.cardwords

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.cardwords.data.local.SqlDelightDatabaseRepository
import com.example.cardwords.db.CardWordsDatabase
import com.example.cardwords.di.AppModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Hide system navigation bar (swipe from bottom edge to reveal temporarily)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val driver = AndroidSqliteDriver(CardWordsDatabase.Schema, applicationContext, "cardwords.db")
        AppModule.initialize(SqlDelightDatabaseRepository(driver) { System.currentTimeMillis() })

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
