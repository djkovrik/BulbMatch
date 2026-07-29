package com.sedsoftware.bulbmatch.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = BulbMatchDatabase.Schema,
        name = BULB_MATCH_DATABASE_NAME,
    )
}
