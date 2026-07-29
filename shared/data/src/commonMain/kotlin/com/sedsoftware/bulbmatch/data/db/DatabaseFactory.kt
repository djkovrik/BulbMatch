package com.sedsoftware.bulbmatch.data.db

import app.cash.sqldelight.db.SqlDriver

const val BULB_MATCH_DATABASE_NAME = "bulbmatch.db"

fun interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

class BulbMatchDatabaseHandle internal constructor(
    val database: BulbMatchDatabase,
    private val driver: SqlDriver,
) {
    fun close() {
        driver.close()
    }
}

object BulbMatchDatabaseFactory {
    fun create(driverFactory: DatabaseDriverFactory): BulbMatchDatabaseHandle {
        val driver = driverFactory.createDriver()
        return try {
            BulbMatchDatabaseHandle(
                database = BulbMatchDatabase(driver),
                driver = driver,
            )
        } catch (failure: Throwable) {
            driver.close()
            throw failure
        }
    }
}

