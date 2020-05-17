package com.joe.quizzy.server.graphql.dataloaders

import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.modules.DataLoaderRegistryFactory
import org.dataloader.DataLoader
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
            val registry = DataLoaderRegistry()
            val dataLoaderOptions = DataLoaderOptions.newOptions().setBatchLoaderContextProvider {
                context
            }
            registry.register(
                "responsegrades",
                DataLoader.newMappedDataLoader(
                    ResponseGradeLoader(
                        gradeDAO
                    ), dataLoaderOptions
                )
            )
            registry.register(
                "usergrades",
                DataLoader.newMappedDataLoader(
                    UserGradeLoader(
                        gradeDAO
                    ), dataLoaderOptions
                )
            )
            registry.register(
                "batchusers",
                DataLoader.newMappedDataLoader(
                    BatchUserLoader(
                        userDAO
                    ), dataLoaderOptions
                )
            )
            registry.register(
                "batchquestions",
                DataLoader.newMappedDataLoader(
                    BatchQuestionLoader(
                        questionDAO
                    ), dataLoaderOptions
                )
            )
            registry.register(
                "questionresponses",
                DataLoader.newMappedDataLoader(QuestionResponseLoader(responseDAO), dataLoaderOptions)
            )
            registry.register(
                "batchinstances",
                DataLoader.newMappedDataLoader(
                    BulkInstanceLoader(
                        instanceDAO
                    ), dataLoaderOptions
                )
            )
        }
    }
}
