package com.joe.quizzy.persistence.modules

import com.joe.quizzy.persistence.api.ThingDAO
import com.joe.quizzy.persistence.impl.ThingDAOJooq
import com.trib3.db.modules.DbModule
import dev.misfitlabs.kotlinguice4.KotlinModule

/**
 * Binds DAO
 */
class QuizzyPersistenceModule : KotlinModule() {
    override fun configure() {
        install(DbModule())
        bind<ThingDAO>().to<ThingDAOJooq>()
    }
}
