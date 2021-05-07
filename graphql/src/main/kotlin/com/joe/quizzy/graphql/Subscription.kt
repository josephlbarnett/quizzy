// package com.joe.quizzy.graphql
//
// import com.coxautodev.graphql.tools.GraphQLQueryResolver
// import com.joe.quizzy.persistence.api.QuestionDAO
// import javax.inject.Inject
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.ExperimentalCoroutinesApi
// import kotlinx.coroutines.stream.consumeAsFlow
// import org.reactivestreams.Publisher
//
// @UseExperimental(ExperimentalCoroutinesApi::class)
// class Subscription
// @Inject
// constructor(
//    private val thingDAO: QuestionDAO
// ) : GraphQLQueryResolver {
//    fun subscribe(): Publisher<Thing> {
//        val stream = thingDAO.stream()
//        return stream.consumeAsFlow()
//            .flowOn(Dispatchers.IO)
//            .asPublisher()
//    }
// }
