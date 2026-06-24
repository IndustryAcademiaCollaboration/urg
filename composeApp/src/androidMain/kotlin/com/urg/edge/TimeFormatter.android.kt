package com.urg.edge

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun Long.toTimeString(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}
