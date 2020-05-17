package com.joe.quizzy.server.modules

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import com.joe.quizzy.server.graphql.Query
import com.trib3.graphql.modules.GraphQLApplicationModule.Companion.GRAPHQL_PACKAGES_BIND_NAME
import com.trib3.graphql.modules.GraphQLApplicationModule.Companion.GRAPHQL_QUERIES_BIND_NAME
import com.trib3.server.modules.TribeApplicationModule.Companion.APPLICATION_RESOURCES_BIND_NAME
import io.dropwizard.auth.AuthDynamicFeature
import org.testng.annotations.Guice
import org.testng.annotations.Test
import javax.inject.Inject
import javax.inject.Named

@Guice(modules = [QuizzyServiceModule::class])
class QuizzyServiceModuleTest
@Inject constructor(
    @Named(GRAPHQL_PACKAGES_BIND_NAME)
    val packages: Set<@JvmSuppressWildcards String>,
    @Named(GRAPHQL_QUERIES_BIND_NAME)
    val queries: Set<@JvmSuppressWildcards Any>,
    @Named(APPLICATION_RESOURCES_BIND_NAME)
    val resources: Set<@JvmSuppressWildcards Any>
) {
    @Test
    fun testResources() {
        assertThat(packages).all {
            contains("com.joe.quizzy.api")
            contains("com.joe.quizzy.server.graphql")
        }
        assertThat(queries.first()).isInstanceOf(Query::class)
        val dynamicAuthFeatures = resources.filterIsInstance<AuthDynamicFeature>()
        assertThat(dynamicAuthFeatures).isNotEmpty()
    }
}
