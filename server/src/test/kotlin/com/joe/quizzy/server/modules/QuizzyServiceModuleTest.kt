package com.joe.quizzy.server.modules

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isInstanceOf
import com.joe.quizzy.server.graphql.Query
import com.trib3.graphql.modules.GraphQLApplicationModule.Companion.GRAPHQL_PACKAGES_BIND_NAME
import com.trib3.graphql.modules.GraphQLApplicationModule.Companion.GRAPHQL_QUERIES_BIND_NAME
import javax.inject.Inject
import javax.inject.Named
import org.testng.annotations.Guice
import org.testng.annotations.Test

@Guice(modules = [QuizzyServiceModule::class])
class QuizzyServiceModuleTest
@Inject constructor(
    @Named(GRAPHQL_PACKAGES_BIND_NAME)
    val packages: Set<@JvmSuppressWildcards String>,
    @Named(GRAPHQL_QUERIES_BIND_NAME)
    val queries: Set<@JvmSuppressWildcards Any>
) {
    @Test
    fun testResources() {
        assertThat(packages).all {
            contains("com.joe.quizzy.api")
            contains("com.joe.quizzy.server.graphql")
        }
        assertThat(queries.first()).isInstanceOf(Query::class)
    }
}
