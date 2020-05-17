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
import com.joe.quizzy.server.graphql.dataloaders.DataLoaderRegistryFactoryProvider
import com.trib3.graphql.modules.GraphQLApplicationModule
import com.trib3.server.filters.CookieTokenAuthFilter
import com.trib3.server.modules.ServletConfig
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.auth.chained.ChainedAuthFilter
import io.dropwizard.servlets.assets.AssetServlet
import java.net.URI
import javax.inject.Named
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class RedirectResource {
    @Path("/")
    @GET
    fun root(): Response {
        return Response.status(Response.Status.FOUND).location(URI("/app/assets")).build()
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
        resourceBinder().addBinding().to<RedirectResource>()
        bind<Hasher>().to<Argon2Hasher>()
        appServletBinder().addBinding().toInstance(
            ServletConfig(
                "AppAssets",
                AssetServlet(
                    "/assets",
                    "/app/assets",
                    "index.html",
                    Charsets.UTF_8
                ),
                listOf("/app/assets", "/app/assets/*")
            )
        )
        appServletBinder().addBinding().toInstance(
            ServletConfig(
                "root-redirect",
                object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        resp.sendRedirect("/app/")
                    }
                }, listOf("")
            )
        )
        dataLoaderRegistryFactoryBinder().setBinding().toProvider<DataLoaderRegistryFactoryProvider>()
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
