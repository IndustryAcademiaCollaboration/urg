package com.urg.edge

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform