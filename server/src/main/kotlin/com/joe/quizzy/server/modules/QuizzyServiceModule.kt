package com.joe.quizzy.server.modules

import com.google.inject.multibindings.ProvidesIntoSet
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.persistence.modules.QuizzyPersistenceModule
import com.joe.quizzy.server.auth.Argon2Hasher
import com.joe.quizzy.server.auth.Hasher
import com.joe.quizzy.server.auth.SessionAuthenticator
import com.joe.quizzy.server.auth.UserAuthenticator
import com.joe.quizzy.server.auth.UserAuthorizer
import com.joe.quizzy.server.auth.UserPrincipal
import com.joe.quizzy.server.graphql.Mutation
import com.joe.quizzy.server.graphql.Query
import com.trib3.graphql.modules.GraphQLApplicationModule
import com.trib3.server.filters.CookieTokenAuthFilter
import com.trib3.server.modules.ServletConfig
import io.dropwizard.auth.Auth
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.auth.chained.ChainedAuthFilter
import io.dropwizard.servlets.assets.AssetServlet
import java.security.Principal
import java.util.Optional
import javax.inject.Named
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class TestResource {
    @Path("/test")
    @GET
    fun test(@Auth user: Optional<Principal>): Any? {
        return user.orElse(null)
    }
}

/**
 * Binds this service's resources
 */
class QuizzyServiceModule : GraphQLApplicationModule() {
    override fun configureApplication() {
        install(QuizzyPersistenceModule())

        graphQLPackagesBinder().addBinding().toInstance("com.joe.quizzy.api")
        graphQLPackagesBinder().addBinding().toInstance("com.joe.quizzy.server.graphql")

        graphQLQueriesBinder().addBinding().to<Query>()
        graphQLMutationsBinder().addBinding().to<Mutation>()
        // graphQLSubscriptionsBinder().addBinding().to<Subscription>()
        resourceBinder().addBinding().to<TestResource>()
        bind<Hasher>().to<Argon2Hasher>()
        appServletBinder().addBinding().toInstance(
            ServletConfig(
                "AppAssets",
                AssetServlet(
                    "/assets",
                    "/assets",
                    "index.html",
                    Charsets.UTF_8
                ),
                listOf("/assets", "/assets/*")
            )
        )
    }

    @ProvidesIntoSet
    @Named(APPLICATION_RESOURCES_BIND_NAME)
    fun getRoleResources(userDAO: UserDAO, sessionDAO: SessionDAO, hasher: Hasher): Any {
        val chainedFeature = ChainedAuthFilter<Any, UserPrincipal>(
            listOf(
                // Use BASIC auth to get a User from credentials
                BasicCredentialAuthFilter.Builder<UserPrincipal>()
                    .setAuthenticator(UserAuthenticator(userDAO, hasher))
                    .setAuthorizer(UserAuthorizer())
                    .buildAuthFilter(),
                // Use cookie-based session auth to get a User from browser session
                CookieTokenAuthFilter.Builder<UserPrincipal>("x-quizzy-session")
                    .setAuthenticator(SessionAuthenticator(sessionDAO, userDAO))
                    .setAuthorizer(UserAuthorizer())
                    .buildAuthFilter()
            )
        )
        return AuthDynamicFeature(
            chainedFeature
        )
    }
}
