package com.joe.quizzy.server.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.server.auth.UserAuthenticator
import com.joe.quizzy.server.auth.UserPrincipal
import com.trib3.graphql.resources.GraphQLResourceContext
import io.dropwizard.auth.basic.BasicCredentials
import mu.KotlinLogging
import java.time.OffsetDateTime
import java.util.Date
import javax.inject.Inject
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.NewCookie

private val log = KotlinLogging.logger {}
private const val COOKIE_NAME = "x-quizzy-session"

/**
 * GraphQL entry point for [Thing] mutations.   Maps the DAO interfaces to the GraphQL models.
 */
class Mutation @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val sessionDAO: SessionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO,
    private val gradeDAO: GradeDAO,
    private val userAuthenticator: UserAuthenticator
) : GraphQLQueryResolver {

    fun login(context: GraphQLResourceContext, email: String, pass: String): Boolean {
        if (context.principal != null) {
            return true // if already logged in, return
        }
        val principal = userAuthenticator.authenticate(BasicCredentials(email, pass)).orElse(null)
        if (principal is UserPrincipal) {
            val userId = principal.user.id
            if (userId != null) {
                val newSession = sessionDAO.save(Session(null, userId, OffsetDateTime.now(), OffsetDateTime.now()))
                context.cookie = NewCookie(
                    COOKIE_NAME,
                    newSession.id.toString(),
                    null,
                    null,
                    1,
                    null,
                    -1, // expire with session
                    null,
                    false,
                    true
                )
                return true
            }
        }
        return false
    }

    fun changePassword(context: GraphQLResourceContext, oldPass: String, newPass: String): Boolean {
        val principal = context.principal
        if (principal is UserPrincipal) {
            val passCheck = userAuthenticator.authenticate(
                BasicCredentials(principal.user.email, oldPass)
            ).orElse(null)
            if (passCheck != null && passCheck.user.id != null && passCheck.user.id == principal.user.id) {
                userDAO.savePassword(passCheck.user, userAuthenticator.hasher.hash(newPass))
                return true
            }
        }
        return false
    }

    fun logout(context: GraphQLResourceContext): Boolean {
        val principal = context.principal
        if (principal is UserPrincipal) {
            context.cookie =
                NewCookie(
                    Cookie(COOKIE_NAME, ""),
                    null,
                    -1,
                    Date(0), // expire 1970
                    false,
                    true
                )
            val session = principal.session
            if (session != null) {
                sessionDAO.delete(session)
            }
            return true
        }
        return false
    }

    fun user(context: GraphQLResourceContext, user: User): User? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin || principal.user.id == user.id) {
                return userDAO.save(user)
            }
        }
        return null
    }

    fun response(context: GraphQLResourceContext, response: Response): Response? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return responseDAO.save(
                response.copy(
                    userId = principal.user.id!!
                )
            )
        }
        return null
    }

    fun grade(context: GraphQLResourceContext, grade: Grade): Grade? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                return gradeDAO.save(grade)
            }
        }
        return null
    }

    fun question(context: GraphQLResourceContext, question: Question): Question? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                return questionDAO.save(question)
            }
        }
        return null
    }
}
