package com.joe.quizzy.persistence.modules

import assertk.assertThat
import assertk.assertions.isNotNull
import com.joe.quizzy.persistence.api.QuestionDAO
import com.trib3.db.config.DbConfig
import javax.inject.Inject
import org.jooq.DSLContext
import org.testng.annotations.Guice
import org.testng.annotations.Test

@Guice(modules = [QuizzyPersistenceModule::class])
class QuizzyPersistenceModuleTest
@Inject constructor(
    val dao: QuestionDAO,
    val ctx: DSLContext,
    val config: DbConfig
) {
    @Test
    fun testInjection() {
        assertThat(dao).isNotNull()
        assertThat(ctx).isNotNull()
        assertThat(config).isNotNull()
    }
}
