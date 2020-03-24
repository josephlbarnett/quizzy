package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.QuestionsRecord
import java.util.UUID
import java.util.stream.Stream
import javax.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext

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
    override fun all(): List<Question> {
        return ctx.select().from(Tables.QUESTIONS).fetchInto(Question::class.java)
    }

    @Timed
    override fun stream(): Stream<Question> {
        return ctx.select().from(Tables.QUESTIONS).fetchSize(1000).fetchStreamInto(Question::class.java)
    }
}
