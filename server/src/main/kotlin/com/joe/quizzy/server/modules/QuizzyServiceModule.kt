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
import com.joe.quizzy.server.mail.GmailServiceModule
import com.joe.quizzy.server.mail.ScheduledEmailBundle
import com.trib3.graphql.modules.GraphQLApplicationModule
import com.trib3.server.filters.CookieTokenAuthFilter
import com.trib3.server.modules.ServletConfig
import com.trib3.server.modules.ServletFilterConfig
import dev.misfitlabs.kotlinguice4.multibindings.KotlinMultibinder
import io.dropwizard.Configuration
import io.dropwizard.ConfiguredBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.auth.chained.ChainedAuthFilter
import io.dropwizard.servlets.assets.AssetServlet
import java.net.URI
import javax.inject.Named
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
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

private const val X_FORWARDED_PROTOCOL = "X-Forwarded-Proto"

class HttpsFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request is HttpServletRequest && response is HttpServletResponse) {
            if (request.getHeader(X_FORWARDED_PROTOCOL) != null) {
                if (request.getHeader(X_FORWARDED_PROTOCOL).indexOf("https") != 0) {
                    response.sendRedirect("https://${request.serverName}${request.requestURI ?: ""}")
                    return
                }
            }
        }
        chain.doFilter(request, response)
    }
}

/**
 * Binds this service's resources
 */
class QuizzyServiceModule : GraphQLApplicationModule() {
    override fun configureApplication() {
        install(QuizzyPersistenceModule())
        install(GmailServiceModule())

        graphQLPackagesBinder().addBinding().toInstance("com.joe.quizzy.api")
        graphQLPackagesBinder().addBinding().toInstance("com.joe.quizzy.server.graphql")

        graphQLQueriesBinder().addBinding().to<Query>()
        graphQLMutationsBinder().addBinding().to<Mutation>()
        // graphQLSubscriptionsBinder().addBinding().to<Subscription>()
        KotlinMultibinder.newSetBinder<ConfiguredBundle<Configuration>>(kotlinBinder).addBinding()
            .to<ScheduledEmailBundle>()
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
                },
                listOf("")
            )
        )
        KotlinMultibinder.newSetBinder<ServletFilterConfig>(kotlinBinder).addBinding().toInstance(
            ServletFilterConfig(HttpsFilter::class.java.simpleName, HttpsFilter::class.java)
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
