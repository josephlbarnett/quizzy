package com.joe.quizzy.server.modules

import com.google.inject.assistedinject.FactoryModuleBuilder
import com.joe.quizzy.graphql.Mutation
import com.joe.quizzy.graphql.Query
import com.joe.quizzy.graphql.auth.Argon2Hasher
import com.joe.quizzy.graphql.auth.Hasher
import com.joe.quizzy.graphql.auth.SessionAuthenticator
import com.joe.quizzy.graphql.auth.UserAuthenticator
import com.joe.quizzy.graphql.auth.UserAuthorizer
import com.joe.quizzy.graphql.dataloaders.DataLoaderRegistryFactoryProvider
import com.joe.quizzy.graphql.groupme.GroupMeResource
import com.joe.quizzy.graphql.groupme.GroupMeServiceFactory
import com.joe.quizzy.graphql.mail.GmailServiceModule
import com.joe.quizzy.graphql.mail.ScheduledEmailBundle
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.persistence.modules.QuizzyPersistenceModule
import com.trib3.graphql.modules.GraphQLApplicationModule
import com.trib3.server.filters.CookieTokenAuthFilter
import com.trib3.server.modules.ServletConfig
import com.trib3.server.modules.ServletFilterConfig
import dev.misfitlabs.kotlinguice4.multibindings.KotlinMultibinder
import io.dropwizard.auth.AuthFilter
import io.dropwizard.auth.Authorizer
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.auth.chained.ChainedAuthFilter
import io.dropwizard.core.Configuration
import io.dropwizard.core.ConfiguredBundle
import io.dropwizard.servlets.assets.AssetServlet
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.net.URI
import java.security.Principal
import javax.inject.Inject
import javax.inject.Provider
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
                    response.sendRedirect("https://${request.serverName}${request.requestURI.orEmpty()}")
                    return
                }
            }
        }
        chain.doFilter(request, response)
    }
}

class QuizzyAuthFilterProvider @Inject constructor(
    val userDAO: UserDAO,
    val sessionDAO: SessionDAO,
    val hasher: Hasher,
    val authorizer: Authorizer<Principal>,
) : Provider<AuthFilter<*, *>> {
    override fun get(): AuthFilter<*, *> {
        return ChainedAuthFilter<Any, Principal>(
            listOf(
                // Use BASIC auth to get a User from credentials
                BasicCredentialAuthFilter.Builder<Principal>()
                    .setAuthenticator(UserAuthenticator(userDAO, hasher))
                    .setAuthorizer(authorizer)
                    .buildAuthFilter(),
                // Use cookie-based session auth to get a User from browser session
                CookieTokenAuthFilter.Builder<Principal>("x-quizzy-session")
                    .setAuthenticator(SessionAuthenticator(sessionDAO, userDAO))
                    .setAuthorizer(authorizer)
                    .buildAuthFilter(),
                CookieTokenAuthFilter.Builder<Principal>("QUIZZY_AUTHORIZATION")
                    .setAuthenticator(SessionAuthenticator(sessionDAO, userDAO))
                    .setAuthorizer(authorizer)
                    .buildAuthFilter(),
            ),
        )
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
        graphQLPackagesBinder().addBinding().toInstance("com.joe.quizzy.graphql")

        graphQLQueriesBinder().addBinding().to<Query>()
        graphQLMutationsBinder().addBinding().to<Mutation>()
        // graphQLSubscriptionsBinder().addBinding().to<Subscription>()
        KotlinMultibinder.newSetBinder<ConfiguredBundle<Configuration>>(kotlinBinder).addBinding()
            .to<ScheduledEmailBundle>()
        resourceBinder().addBinding().to<RedirectResource>()
        resourceBinder().addBinding().to<GroupMeResource>()
        install(FactoryModuleBuilder().build(GroupMeServiceFactory::class.java))
        bind<Hasher>().to<Argon2Hasher>()
        appServletBinder().addBinding().toInstance(
            ServletConfig(
                "AppAssets",
                AssetServlet(
                    "/assets",
                    "/app/assets",
                    "index.html",
                    Charsets.UTF_8,
                ),
                listOf("/app/assets", "/app/assets/*"),
            ),
        )
        appServletBinder().addBinding().toInstance(
            ServletConfig(
                "root-redirect",
                object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        resp.sendRedirect("/app/")
                    }
                },
                listOf(""),
            ),
        )
        KotlinMultibinder.newSetBinder<ServletFilterConfig>(kotlinBinder).addBinding().toInstance(
            ServletFilterConfig(HttpsFilter::class.java.simpleName, HttpsFilter::class.java),
        )
        dataLoaderRegistryFactoryProviderBinder().setBinding().toProvider<DataLoaderRegistryFactoryProvider>()
        authorizerBinder().setBinding().to<UserAuthorizer>()
        authFilterBinder().setBinding().toProvider<QuizzyAuthFilterProvider>()
        bind<HttpClient>().toInstance(
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    jackson()
                }
            },
        )
    }
}
