package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.AnswerChoice
import com.joe.quizzy.api.models.NotificationType
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.QuestionType
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import jakarta.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.ResultQuery
import org.jooq.SelectConditionStep
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class QuestionDAOJooq
    @Inject
    constructor(
        private val ctx: DSLContext,
    ) : QuestionDAO {
        val questionsAndChoices =
            Tables.QUESTIONS
                .leftJoin(
                    Tables.ANSWER_CHOICES,
                ).on(Tables.QUESTIONS.ID.eq(Tables.ANSWER_CHOICES.QUESTION_ID))

        val collector: Collector<Record, *, Map<Question, List<AnswerChoice>>> =
            Collectors.groupingBy(
                { it.into(Tables.QUESTIONS).into(Question::class.java) },
                ::LinkedHashMap, // use a linked hashmap to preserve the db-returned order
                Collectors.filtering(
                    { it.get(Tables.ANSWER_CHOICES.ID) != null },
                    Collectors.mapping(
                        {
                            val answerRecord = it.into(Tables.ANSWER_CHOICES)
                            answerRecord.into(AnswerChoice::class.java)
                        },
                        Collectors.toList(),
                    ),
                ),
            )

        val collectAndMap: ResultQuery<Record>.() -> List<Question> = {
            this
                .collect(collector)
                .map { it.key.copy(answerChoices = it.value.filterNotNull().sortedBy(AnswerChoice::letter)) }
        }

        @Timed
        override fun get(id: UUID): Question? =
            ctx
                .selectFrom(questionsAndChoices)
                .where(Tables.QUESTIONS.ID.eq(id))
                .collectAndMap()
                .firstOrNull()

        @Timed
        override fun get(ids: List<UUID>): List<Question> {
            val query = ctx.selectFrom(questionsAndChoices).where(Tables.QUESTIONS.ID.`in`(ids))
            log.info("batch get questions: $query")
            return query.collectAndMap()
        }

        @Timed
        override fun save(thing: Question): Question =
            ctx.transactionResult { config ->
                val thingId = thing.id
                val record =
                    if (thingId == null) {
                        config.dsl().newRecord(
                            Tables.QUESTIONS,
                            thing,
                        )
                    } else {
                        val existing =
                            config
                                .dsl()
                                .selectFrom(Tables.QUESTIONS)
                                .where(Tables.QUESTIONS.ID.eq(thingId))
                                .fetchOne()
                        if (existing != null) {
                            existing.from(thing)
                            existing
                        } else {
                            config.dsl().newRecord(
                                Tables.QUESTIONS,
                                thing,
                            )
                        }
                    }
                record.store()
                val returnedQuestion = record.into(Question::class.java)
                if (thing.type == QuestionType.MULTIPLE_CHOICE) {
                    config
                        .dsl()
                        .delete(Tables.ANSWER_CHOICES)
                        .where(Tables.ANSWER_CHOICES.QUESTION_ID.eq(record.id))
                        .execute()
                    val savedChoices =
                        thing.answerChoices?.map {
                            val choiceRecord = config.dsl().newRecord(Tables.ANSWER_CHOICES, it)
                            choiceRecord.questionId = returnedQuestion.id
                            choiceRecord.store()
                            choiceRecord.into(AnswerChoice::class.java)
                        }
                    check(savedChoices?.any { it.letter == returnedQuestion.answer } ?: true) {
                        "No valid answer selected"
                    }
                    returnedQuestion.copy(answerChoices = savedChoices)
                } else {
                    returnedQuestion
                }
            }

        @Timed
        private fun instanceQuestions(user: User): SelectOnConditionStep<Record> =
            ctx
                .select()
                .from(questionsAndChoices)
                .join(Tables.USERS)
                .on(
                    Tables.USERS.ID
                        .eq(Tables.QUESTIONS.AUTHOR_ID)
                        .and(Tables.USERS.INSTANCE_ID.eq(user.instanceId)),
                )

        private fun gradeExists(user: User): SelectConditionStep<Record> =
            ctx
                .select()
                .from(Tables.GRADES)
                .leftJoin(Tables.RESPONSES)
                .on(Tables.GRADES.RESPONSE_ID.eq(Tables.RESPONSES.ID))
                .where(
                    Tables.RESPONSES.USER_ID.eq(user.id).and(
                        Tables.RESPONSES.QUESTION_ID.eq(Tables.QUESTIONS.ID),
                    ),
                )

        @Timed
        override fun active(user: User): List<Question> {
            val now = OffsetDateTime.now()
            val query =
                instanceQuestions(user)
                    .where(
                        Tables.QUESTIONS.ACTIVE_AT
                            .le(now)
                            .and(Tables.QUESTIONS.CLOSED_AT.ge(now))
                            .andNotExists(
                                gradeExists(user),
                            ),
                    ).orderBy(Tables.QUESTIONS.ACTIVE_AT)
            log.info("active questions query: $query")
            return query.collectAndMap()
        }

        override fun active(notificationType: NotificationType): List<Question> {
            val now = OffsetDateTime.now()
            val query =
                ctx
                    .select()
                    .from(
                        questionsAndChoices
                            .leftJoin(Tables.EMAIL_NOTIFICATIONS)
                            .on(
                                Tables.QUESTIONS.ID
                                    .eq(Tables.EMAIL_NOTIFICATIONS.QUESTION_ID)
                                    .and(Tables.EMAIL_NOTIFICATIONS.NOTIFICATION_TYPE.eq(notificationType.name)),
                            ),
                    ).where(
                        Tables.QUESTIONS.ACTIVE_AT
                            .le(now)
                            .and(Tables.QUESTIONS.CLOSED_AT.ge(now))
                            .and(Tables.EMAIL_NOTIFICATIONS.ID.isNull),
                    ).orderBy(Tables.QUESTIONS.ACTIVE_AT)
            return query.collectAndMap()
        }

        @Timed
        override fun closed(
            user: User,
            startTime: OffsetDateTime?,
            endTime: OffsetDateTime?,
        ): List<Question> {
            val now = OffsetDateTime.now()
            val query =
                instanceQuestions(user)
                    .where(
                        DSL.and(
                            listOfNotNull(
                                Tables.QUESTIONS.CLOSED_AT.le(now).orExists(
                                    gradeExists(user),
                                ),
                                startTime?.let {
                                    Tables.QUESTIONS.CLOSED_AT.ge(it)
                                },
                                endTime?.let {
                                    Tables.QUESTIONS.ACTIVE_AT.le(it)
                                },
                            ),
                        ),
                    ).orderBy(Tables.QUESTIONS.ACTIVE_AT.desc())
            log.info("closed questions query: $query")
            return query.collectAndMap()
        }

        override fun closed(notificationType: NotificationType): List<Question> {
            val now = OffsetDateTime.now()
            val query =
                ctx
                    .select()
                    .from(
                        questionsAndChoices
                            .leftJoin(Tables.EMAIL_NOTIFICATIONS)
                            .on(
                                Tables.QUESTIONS.ID
                                    .eq(Tables.EMAIL_NOTIFICATIONS.QUESTION_ID)
                                    .and(Tables.EMAIL_NOTIFICATIONS.NOTIFICATION_TYPE.eq(notificationType.name)),
                            ).join(Tables.USERS)
                            .on(Tables.QUESTIONS.AUTHOR_ID.eq(Tables.USERS.ID))
                            .join(Tables.INSTANCES)
                            .on(Tables.USERS.INSTANCE_ID.eq(Tables.INSTANCES.ID)),
                    ).where(
                        Tables.QUESTIONS.CLOSED_AT
                            .le(now)
                            .and(Tables.EMAIL_NOTIFICATIONS.ID.isNull)
                            .and(DSL.not(Tables.INSTANCES.AUTO_GRADE.isTrue)),
                    ).orderBy(Tables.QUESTIONS.ACTIVE_AT.desc())
            return query.collectAndMap()
        }

        @Timed
        override fun future(user: User): List<Question> {
            val now = OffsetDateTime.now()
            val query =
                instanceQuestions(user)
                    .where(Tables.QUESTIONS.ACTIVE_AT.gt(now))
                    .orderBy(Tables.QUESTIONS.ACTIVE_AT)
            log.info("future questions query: $query")
            return query.collectAndMap()
        }

        @Timed
        override fun all(): List<Question> =
            ctx
                .select()
                .from(questionsAndChoices)
                .orderBy(Tables.QUESTIONS.ACTIVE_AT)
                .collectAndMap()

        @Timed
        override fun stream(): Stream<Question> =
            ctx
                .select()
                .from(Tables.QUESTIONS)
                .orderBy(Tables.QUESTIONS.ACTIVE_AT)
                .fetchStreamInto(Question::class.java)
    }
