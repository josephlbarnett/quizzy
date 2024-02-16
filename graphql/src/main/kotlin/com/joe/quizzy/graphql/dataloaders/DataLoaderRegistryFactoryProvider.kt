package com.joe.quizzy.graphql.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.joe.quizzy.graphql.groupme.GroupMeServiceFactory
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SeasonDAO
import com.joe.quizzy.persistence.api.UserDAO
import jakarta.inject.Inject
import jakarta.inject.Provider

/**
 * Provide a DataLoaderRegistry per request that forwards
 * the graphQL context object to each dataloader registered.
 * DataLoaders that fetch child objects are MappedDataLoaders,
 * DataLoaders that fetch parents are regular DataLoaders
 */
class DataLoaderRegistryFactoryProvider
    @Inject
    constructor(
        private val gradeDAO: GradeDAO,
        private val userDAO: UserDAO,
        private val questionDAO: QuestionDAO,
        private val responseDAO: ResponseDAO,
        private val instanceDAO: InstanceDAO,
        private val seasonDAO: SeasonDAO,
        private val groupMeServiceFactory: GroupMeServiceFactory,
    ) : Provider<KotlinDataLoaderRegistryFactory> {
        override fun get(): KotlinDataLoaderRegistryFactory {
            return KotlinDataLoaderRegistryFactory(
                ResponseGradeLoader(gradeDAO),
                UserGradeLoader(gradeDAO),
                BatchUserLoader(userDAO),
                BatchQuestionLoader(questionDAO),
                QuestionResponseLoader(responseDAO),
                BulkInstanceLoader(instanceDAO),
                InstanceSeasonLoader(seasonDAO),
                GroupMeServiceLoader(groupMeServiceFactory),
            )
        }
    }
