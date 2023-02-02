package com.joe.quizzy.persistence.impl

import com.trib3.testing.db.DAOTestBase
import org.flywaydb.core.api.configuration.FluentConfiguration
import java.time.OffsetDateTime
import java.time.ZoneOffset

val EARLY_START_TIME = OffsetDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
val EARLY_END_TIME = OffsetDateTime.of(1990, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

/**
 * Configure Flyway for .postgresql files
 */
open class PostgresDAOTestBase : DAOTestBase() {
    override fun getFlywayConfiguration(): FluentConfiguration {
        return super.getFlywayConfiguration().sqlMigrationSuffixes(".sql", ".postgresql")
    }
}
