package com.joe.quizzy.persistence.modules

import com.joe.quizzy.persistence.api.EmailNotificationDAO
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.persistence.impl.EmailNotificationDAOJooq
import com.joe.quizzy.persistence.impl.GradeDAOJooq
import com.joe.quizzy.persistence.impl.InstanceDAOJooq
import com.joe.quizzy.persistence.impl.QuestionDAOJooq
import com.joe.quizzy.persistence.impl.ResponseDAOJooq
import com.joe.quizzy.persistence.impl.SessionDAOJooq
import com.joe.quizzy.persistence.impl.UserDAOJooq
import com.trib3.db.modules.DbModule
import com.trib3.db.modules.FlywayModule
import dev.misfitlabs.kotlinguice4.KotlinModule
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

/**
 * Binds DAO
 */
class QuizzyPersistenceModule : KotlinModule() {
    override fun configure() {
        install(DbModule())
        install(FlywayModule())
        bind<FluentConfiguration>().toInstance(Flyway.configure().sqlMigrationSuffixes(".sql", ".postgresql"))
        bind<QuestionDAO>().to<QuestionDAOJooq>()
        bind<ResponseDAO>().to<ResponseDAOJooq>()
        bind<SessionDAO>().to<SessionDAOJooq>()
        bind<UserDAO>().to<UserDAOJooq>()
        bind<GradeDAO>().to<GradeDAOJooq>()
        bind<InstanceDAO>().to<InstanceDAOJooq>()
        bind<EmailNotificationDAO>().to<EmailNotificationDAOJooq>()
    }
}
