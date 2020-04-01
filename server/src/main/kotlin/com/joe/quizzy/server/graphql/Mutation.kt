package com.joe.quizzy.server.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.server.auth.UserAuthenticator
import com.joe.quizzy.server.auth.UserPrincipal
import com.trib3.graphql.resources.GraphQLResourceContext
import io.dropwizard.auth.basic.BasicCredentials
import java.time.OffsetDateTime
import java.util.Date
import javax.inject.Inject
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.NewCookie
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * GraphQL entry point for [Thing] mutations.   Maps the DAO interfaces to the GraphQL models.
 */
class Mutation @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val sessionDAO: SessionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO,
    private val userAuthenticator: UserAuthenticator
) : GraphQLQueryResolver {

    fun login(context: GraphQLResourceContext, email: String, pass: String): Boolean {
        log.info("logging in with context $context")
        if (context.principal != null) {
            return true // if already logged in, return
        }
        val principal = userAuthenticator.authenticate(BasicCredentials(email, pass)).orElse(null)
        val userId = if (principal is UserPrincipal) {
            principal.user.id
        } else {
            null
        }
        if (userId != null) {
            val newSession = sessionDAO.save(Session(null, userId, OffsetDateTime.now(), OffsetDateTime.now()))
            context.cookie = NewCookie("x-quizzy-session", newSession.id.toString())
            return true
        }
        return false
    }

    fun logout(context: GraphQLResourceContext): Boolean {
        if (context.principal != null) {
            context.cookie =
                NewCookie(
                    Cookie("x-quizzy-session", ""),
                    null,
                    -1,
                    Date(0), // 1970
                    false,
                    false
                )
            return true
        }
        return false
    }

    fun user(context: GraphQLResourceContext, thing: User): User? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin || principal.user.id == thing.id) {
                return userDAO.save(thing)
            }
        }
        return null
    }

    fun response(context: GraphQLResourceContext, thing: Response): Response? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            // only let admins set bonus // correct
            if (principal.user.admin || (thing.bonus == null && thing.correct == null)) {
                return responseDAO.save(
                    thing.copy(
                        userId = principal.user.id!!
                    )
                )
            }
        }
        return null
    }

    fun question(context: GraphQLResourceContext, thing: Question): Question? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return questionDAO.save(thing)
        }
        return null
    }
}
