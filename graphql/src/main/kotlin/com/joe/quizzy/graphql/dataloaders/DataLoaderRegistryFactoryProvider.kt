package com.joe.quizzy.graphql.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.modules.KotlinDataLoaderRegistryFactoryProvider
import javax.inject.Inject
import javax.inject.Provider

/**
 * Provide a DataLoaderRegistry per request that forwards
 * the graphQL context object to each dataloader registered.
 * DataLoaders that fetch child objects are MappedDataLoaders,
 * DataLoaders that fetch parents are regular DataLoaders
 */
class DataLoaderRegistryFactoryProvider @Inject constructor(
    private val gradeDAO: GradeDAO,
    private val userDAO: UserDAO,
    private val questionDAO: QuestionDAO,
    private val responseDAO: ResponseDAO,
    private val instanceDAO: InstanceDAO,
) : Provider<KotlinDataLoaderRegistryFactoryProvider> {
    override fun get(): KotlinDataLoaderRegistryFactoryProvider {
        return { _, contextMap ->
            KotlinDataLoaderRegistryFactory(
                ResponseGradeLoader(gradeDAO, contextMap),
                UserGradeLoader(gradeDAO, contextMap),
                BatchUserLoader(userDAO, contextMap),
                BatchQuestionLoader(questionDAO, contextMap),
                QuestionResponseLoader(responseDAO, contextMap),
                BulkInstanceLoader(instanceDAO, contextMap),
            )
        }
    }
}
