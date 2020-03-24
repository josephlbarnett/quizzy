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
import java.time.LocalDateTime
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
            val newSession = sessionDAO.save(Session(null, userId, LocalDateTime.now(), LocalDateTime.now()))
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

    fun user(thing: User): User {
        return userDAO.save(thing)
    }

    fun response(thing: Response): Response {
        return responseDAO.save(thing)
    }

    fun question(thing: Question): Question {
        return questionDAO.save(thing)
    }
}
