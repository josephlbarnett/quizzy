package com.joe.quizzy.server.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.joe.quizzy.api.models.Thing
import com.joe.quizzy.persistence.api.ThingDAO
import javax.inject.Inject

/**
 * GraphQL entry point for [Thing] mutations.   Maps the DAO interfaces to the GraphQL models.
 */
class Mutation @Inject constructor(
    private val thingDAO: ThingDAO
) : GraphQLQueryResolver {
    fun thing(thing: Thing): Thing {
        return thingDAO.save(thing)
    }
}
