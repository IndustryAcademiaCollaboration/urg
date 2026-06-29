package com.urg.edge

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.timeZoneWithName

actual fun Long.toTimeString(): String {
    val date = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "HH:mm:ss"
    formatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Tokyo") ?: NSTimeZone.localTimeZone
    return formatter.stringFromDate(date)
}
