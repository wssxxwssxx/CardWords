package com.example.cardwords

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform