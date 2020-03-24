package com.joe.quizzy.persistence.impl

import com.trib3.testing.db.DAOTestBase
import org.flywaydb.core.api.configuration.FluentConfiguration

/**
 * Configure Flyway for .postgresql files
 */
open class PostgresDAOTestBase : DAOTestBase() {
    override fun getFlywayConfiguration(): FluentConfiguration {
        return super.getFlywayConfiguration().sqlMigrationSuffixes(".sql", ".postgresql")
    }
}
