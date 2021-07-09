package com.joe.quizzy.graphql.dataloaders

import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.modules.DataLoaderRegistryFactory
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderOptions
import org.dataloader.DataLoaderRegistry
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
    private val instanceDAO: InstanceDAO
) : Provider<DataLoaderRegistryFactory> {
    override fun get(): DataLoaderRegistryFactory {
        return { _, context ->
            val dataLoaderOptions = DataLoaderOptions.newOptions().setBatchLoaderContextProvider {
                context
            }
            DataLoaderRegistry.newRegistry()
                .register(
                    "responsegrades",
                    DataLoaderFactory.newMappedDataLoader(ResponseGradeLoader(gradeDAO), dataLoaderOptions)
                )
                .register(
                    "usergrades",
                    DataLoaderFactory.newMappedDataLoader(UserGradeLoader(gradeDAO), dataLoaderOptions)
                )
                .register(
                    "batchusers",
                    DataLoaderFactory.newMappedDataLoader(BatchUserLoader(userDAO), dataLoaderOptions)
                )
                .register(
                    "batchquestions",
                    DataLoaderFactory.newMappedDataLoader(BatchQuestionLoader(questionDAO), dataLoaderOptions)
                )
                .register(
                    "questionresponses",
                    DataLoaderFactory.newMappedDataLoader(QuestionResponseLoader(responseDAO), dataLoaderOptions)
                )
                .register(
                    "batchinstances",
                    DataLoaderFactory.newMappedDataLoader(BulkInstanceLoader(instanceDAO), dataLoaderOptions)
                )
                .build()
        }
    }
}
