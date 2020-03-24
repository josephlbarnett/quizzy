package com.joe.quizzy.server.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.resources.GraphQLResourceContext
import java.util.UUID
import javax.inject.Inject
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * GraphQL entry point for [Thing] queries.  Maps the DAO interfaces to the GraphQL models.
 */
class Query @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO
) : GraphQLQueryResolver {

    fun user(possibleUser: GraphQLResourceContext, thing: UUID): User? {
        log.info("Getting users with context $possibleUser")
        return userDAO.get(thing)
    }

    fun response(thing: UUID): Response? {
        return responseDAO.get(thing)
    }

    fun question(thing: UUID): Question? {
        return questionDAO.get(thing)
    }
}
