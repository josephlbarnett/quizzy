package com.joe.quizzy.server.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.joe.quizzy.api.models.Thing
import com.joe.quizzy.persistence.api.ThingDAO
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.stream.consumeAsFlow
import org.reactivestreams.Publisher

@UseExperimental(ExperimentalCoroutinesApi::class)
class Subscription
@Inject
constructor(
    private val thingDAO: ThingDAO
) : GraphQLQueryResolver {
    fun subscribe(): Publisher<Thing> {
        val stream = thingDAO.stream()
        return stream.consumeAsFlow()
            .flowOn(Dispatchers.IO)
            .asPublisher()
    }
}
