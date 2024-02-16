package com.joe.quizzy.server.modules

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.joe.quizzy.graphql.Query
import com.trib3.graphql.modules.GraphQLApplicationModule.Companion.GRAPHQL_PACKAGES_BIND_NAME
import com.trib3.graphql.modules.GraphQLApplicationModule.Companion.GRAPHQL_QUERIES_BIND_NAME
import io.dropwizard.auth.AuthFilter
import jakarta.inject.Inject
import jakarta.inject.Named
import org.testng.annotations.Guice
import org.testng.annotations.Test

@Guice(modules = [QuizzyServiceModule::class])
class QuizzyServiceModuleTest
    @Inject
    constructor(
        @Named(GRAPHQL_PACKAGES_BIND_NAME)
        val packages: Set<String>,
        @Named(GRAPHQL_QUERIES_BIND_NAME)
        val queries: Set<Any>,
        val authFilter: AuthFilter<*, *>,
    ) {
        @Test
        fun testResources() {
            assertThat(packages).all {
                contains("com.joe.quizzy.api")
                contains("com.joe.quizzy.graphql")
            }
            assertThat(queries.first()).isInstanceOf(Query::class)
            assertThat(authFilter).isNotNull()
        }
    }
