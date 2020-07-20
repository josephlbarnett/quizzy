package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.QuestionsRecord
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.SelectOnConditionStep
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream
import javax.inject.Inject

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class QuestionDAOJooq
@Inject constructor(
    private val ctx: DSLContext
) : QuestionDAO {
    private fun getRecord(dsl: DSLContext, id: UUID): QuestionsRecord? {
        return dsl.selectFrom(Tables.QUESTIONS).where(Tables.QUESTIONS.ID.eq(id)).fetchOne()
    }

    @Timed
    override fun get(id: UUID): Question? {
        return getRecord(ctx, id)?.into(Question::class.java)
    }

    @Timed
    override fun get(ids: List<UUID>): List<Question> {
        val query = ctx.selectFrom(Tables.QUESTIONS).where(Tables.QUESTIONS.ID.`in`(ids))
        log.info("batch get questions: $query")
        return query.fetchInto(Question::class.java)
    }

    @Timed
    override fun save(thing: Question): Question {
        return ctx.transactionResult { config ->
            val thingId = thing.id
            val record = if (thingId == null) {
                config.dsl().newRecord(
                    Tables.QUESTIONS,
                    thing
                )
            } else {
                val existing = getRecord(config.dsl(), thingId)
                if (existing != null) {
                    existing.from(thing)
                    existing
                } else {
                    config.dsl().newRecord(
                        Tables.QUESTIONS,
                        thing
                    )
                }
            }
            record.store()
            record.into(Question::class.java)
        }
    }

    @Timed
    private fun instanceQuestions(user: User): SelectOnConditionStep<Record> {
        return ctx.select(Tables.QUESTIONS.asterisk()).from(Tables.QUESTIONS)
            .join(Tables.USERS)
            .on(
                Tables.USERS.ID.eq(Tables.QUESTIONS.AUTHOR_ID)
                    .and(Tables.USERS.INSTANCE_ID.eq(user.instanceId))
            )
    }

    @Timed
    override fun active(): List<Question> {
        val now = OffsetDateTime.now()
        val query = ctx.select(Tables.QUESTIONS.asterisk()).from(Tables.QUESTIONS)
            .where(
                Tables.QUESTIONS.ACTIVE_AT.le(now)
                    .and(Tables.QUESTIONS.CLOSED_AT.ge(now))
            ).orderBy(Tables.QUESTIONS.CLOSED_AT)
        return query.fetchInto(Question::class.java)
    }

    @Timed
    override fun active(user: User): List<Question> {
        val now = OffsetDateTime.now()
        val query = instanceQuestions(user)
            .where(
                Tables.QUESTIONS.ACTIVE_AT.le(now)
                    .and(Tables.QUESTIONS.CLOSED_AT.ge(now))
            ).orderBy(Tables.QUESTIONS.CLOSED_AT)
        log.info("active questions query: $query")
        return query.fetchInto(Question::class.java)
    }

    @Timed
    override fun closed(): List<Question> {
        val now = OffsetDateTime.now()
        val query = ctx.select(Tables.QUESTIONS.asterisk()).from(Tables.QUESTIONS)
            .where(Tables.QUESTIONS.CLOSED_AT.le(now)).orderBy(Tables.QUESTIONS.CLOSED_AT)
        return query.fetchInto(Question::class.java)
    }

    @Timed
    override fun closed(user: User): List<Question> {
        val now = OffsetDateTime.now()
        val query = instanceQuestions(user)
            .where(Tables.QUESTIONS.CLOSED_AT.le(now)).orderBy(Tables.QUESTIONS.CLOSED_AT)
        log.info("closed questions query: $query")
        return query.fetchInto(Question::class.java)
    }

    @Timed
    override fun future(user: User): List<Question> {
        val now = OffsetDateTime.now()
        val query = instanceQuestions(user)
            .where(Tables.QUESTIONS.ACTIVE_AT.gt(now)).orderBy(Tables.QUESTIONS.CLOSED_AT)
        log.info("future questions query: $query")
        return query.fetchInto(Question::class.java)
    }

    @Timed
    override fun all(): List<Question> {
        return ctx.select().from(Tables.QUESTIONS).orderBy(Tables.QUESTIONS.CLOSED_AT).fetchInto(Question::class.java)
    }

    @Timed
    override fun stream(): Stream<Question> {
        return ctx.select().from(Tables.QUESTIONS).orderBy(Tables.QUESTIONS.CLOSED_AT).fetchSize(1000)
            .fetchStreamInto(Question::class.java)
    }
}
