package com.urg.edge.core.database

import com.urg.edge.database.UrgDatabase

class DatabaseFactory(private val driverFactory: DatabaseDriverFactory) {
    fun createDatabase(): UrgDatabase {
        return UrgDatabase(driverFactory.createDriver())
    }
}