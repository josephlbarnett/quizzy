package com.joe.quizzy.server.modules

import com.joe.quizzy.persistence.modules.QuizzyPersistenceModule
import com.joe.quizzy.server.graphql.Mutation
import com.joe.quizzy.server.graphql.Query
import com.joe.quizzy.server.graphql.Subscription
import com.trib3.graphql.modules.GraphQLApplicationModule

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
        graphQLSubscriptionsBinder().addBinding().to<Subscription>()
    }
}
