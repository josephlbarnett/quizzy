package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.QuestionType
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.ResponsesRecord
import jakarta.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class ResponseDAOJooq
@Inject constructor(
    private val ctx: DSLContext,
    private val gradeDAO: GradeDAOJooq,
    private val instanceDAO: InstanceDAO,
    private val questionDAO: QuestionDAO,
    private val userDAO: UserDAO,
) : ResponseDAO {
    private fun getRecord(dsl: DSLContext, id: UUID): ResponsesRecord? {
        return dsl.selectFrom(Tables.RESPONSES).where(Tables.RESPONSES.ID.eq(id)).fetchOne()
    }

    @Timed
    override fun get(id: UUID): Response? {
        return getRecord(ctx, id)?.into(Response::class.java)
    }

    @Timed
    override fun save(thing: Response): Response {
        return ctx.transactionResult { config ->
            val thingId = thing.id
            val record = if (thingId == null) {
                config.dsl().newRecord(
                    Tables.RESPONSES,
                    thing,
                )
            } else {
                val existing = getRecord(config.dsl(), thingId)
                if (existing != null) {
                    existing.from(thing)
                    existing
                } else {
                    config.dsl().newRecord(
                        Tables.RESPONSES,
                        thing,
                    )
                }
            }

            record.store()
            questionDAO.get(record.questionId)?.let { question ->
                userDAO.get(question.authorId)?.let { author ->
                    instanceDAO.get(author.instanceId)?.let { instance ->
                        if (instance.autoGrade && question.type == QuestionType.MULTIPLE_CHOICE) {
                            val correctAnswer = question.answerChoices?.firstOrNull { it.letter == question.answer }
                            if (correctAnswer != null) {
                                val existingGrade = gradeDAO.forResponse(record.id)
                                val grade = Grade(
                                    existingGrade?.id,
                                    record.id,
                                    record.response == correctAnswer.letter,
                                    null,
                                )
                                gradeDAO.save(grade, config)
                            }
                        }
                    }
                }
            }
            record.into(Response::class.java)
        }
    }

    @Timed
    override fun byUserQuestion(userId: UUID, questionId: UUID): Response? {
        val query = ctx.select(Tables.RESPONSES.asterisk()).from(Tables.RESPONSES)
            .where(Tables.RESPONSES.USER_ID.eq(userId).and(Tables.RESPONSES.QUESTION_ID.eq(questionId)))
        log.info("user question responses query : $query")
        return query.fetchOneInto(Response::class.java)
    }

    @Timed
    override fun byUserQuestions(userId: UUID, questionIds: List<UUID>): Map<UUID, Response> {
        val query = ctx.select(Tables.RESPONSES.asterisk()).from(Tables.RESPONSES)
            .where(Tables.RESPONSES.USER_ID.eq(userId).and(Tables.RESPONSES.QUESTION_ID.`in`(questionIds)))
        log.info("batch user question responses query : $query")
        return query.fetch().intoMap(Tables.RESPONSES.QUESTION_ID, Response::class.java)
    }

    @Timed
    override fun forInstance(
        instanceId: UUID,
        regrade: Boolean,
        startTime: OffsetDateTime?,
        endTime: OffsetDateTime?,
    ): List<Response> {
        val initialQuery = ctx.select(Tables.RESPONSES.asterisk()).from(Tables.RESPONSES)
            .join(Tables.USERS).on(
                Tables.RESPONSES.USER_ID.eq(Tables.USERS.ID).and(
                    Tables.USERS.INSTANCE_ID.eq(instanceId),
                ),
            )
            .join(Tables.QUESTIONS).on(
                Tables.QUESTIONS.ID.eq(Tables.RESPONSES.QUESTION_ID),
            )
        val query = if (regrade) {
            initialQuery.where()
        } else {
            initialQuery.leftJoin(Tables.GRADES).on(
                Tables.GRADES.RESPONSE_ID.eq(Tables.RESPONSES.ID),
            ).where(Tables.GRADES.CORRECT.isNull)
        }
            .and(
                DSL.and(
                    listOfNotNull(
                        startTime?.let {
                            Tables.QUESTIONS.CLOSED_AT.ge(it)
                        },
                        endTime?.let {
                            Tables.QUESTIONS.ACTIVE_AT.le(it)
                        },
                    ),
                ),
            )
            .orderBy(Tables.QUESTIONS.ACTIVE_AT.desc(), Tables.USERS.NAME)

        log.info("graded query : $query")
        return query.fetchInto(Response::class.java)
    }

    @Timed
    override fun forUser(userId: UUID): List<Response> {
        val query =
            ctx.select(Tables.RESPONSES.asterisk()).from(Tables.RESPONSES).where(Tables.RESPONSES.USER_ID.eq(userId))
        log.info("user responses query : $query")
        return query.fetchInto(Response::class.java)
    }

    @Timed
    override fun all(): List<Response> {
        return ctx.select().from(Tables.RESPONSES).fetchInto(Response::class.java)
    }

    @Timed
    override fun stream(): Stream<Response> {
        return ctx.select().from(Tables.RESPONSES).fetchStreamInto(Response::class.java)
    }
}
