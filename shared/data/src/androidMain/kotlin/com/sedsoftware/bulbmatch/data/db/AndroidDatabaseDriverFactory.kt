package com.sedsoftware.bulbmatch.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidDatabaseDriverFactory(
    context: Context,
) : DatabaseDriverFactory {
    private val applicationContext = context.applicationContext

    override fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = BulbMatchDatabase.Schema,
        context = applicationContext,
        name = BULB_MATCH_DATABASE_NAME,
    )
}

