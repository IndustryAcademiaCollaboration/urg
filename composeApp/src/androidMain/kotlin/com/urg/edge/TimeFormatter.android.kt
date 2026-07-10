package com.urg.edge

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual fun Long.toTimeString(): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPAN)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
    return sdf.format(Date(this))
}
