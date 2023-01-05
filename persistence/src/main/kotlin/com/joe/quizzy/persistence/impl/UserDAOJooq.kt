package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.Tables.USER_INVITE
import com.joe.quizzy.persistence.impl.jooq.tables.records.UsersRecord
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.util.UUID
import java.util.stream.Stream
import javax.inject.Inject

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class UserDAOJooq
@Inject constructor(
    private val ctx: DSLContext,
) : UserDAO {
    private fun getRecord(dsl: DSLContext, id: UUID): UsersRecord? {
        return dsl.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(id)).fetchOne()
    }

    @Timed
    override fun get(id: UUID): User? {
        return getRecord(ctx, id)?.into(User::class.java)
    }

    @Timed
    override fun get(ids: List<UUID>): List<User> {
        val query = ctx.selectFrom(Tables.USERS).where(Tables.USERS.ID.`in`(ids))
        log.info("Batch get users: $query")
        return query.fetch()
            .into(User::class.java)
    }

    @Timed
    override fun getByEmail(email: String): User? {
        return ctx.selectFrom(Tables.USERS).where(Tables.USERS.EMAIL.eq(email)).fetchOneInto(User::class.java)
    }

    @Timed
    override fun getByInstance(instanceId: UUID): List<User> {
        val query = ctx.selectFrom(Tables.USERS).where(Tables.USERS.INSTANCE_ID.eq(instanceId))
            .orderBy(DSL.lower(Tables.USERS.NAME))
        log.info("get users by instance: $query")
        return query
            .fetchInto(User::class.java)
    }

    @Timed
    override fun savePassword(user: User, cryptedPass: String): Int {
        return ctx.transactionResult { config ->
            config.dsl().update(Tables.USERS).set(Tables.USERS.AUTH_CRYPT, cryptedPass)
                .set(Tables.USERS.PASSWORD_RESET_TOKEN, null as String?)
                .where(Tables.USERS.ID.eq(user.id))
                .execute().also {
                    check(it == 1) {
                        "Password update updated $it rows, rolling back"
                    }
                }
        }
    }

    @Timed
    override fun save(thing: User): User {
        return ctx.transactionResult { config ->
            val thingId = thing.id
            val record = if (thingId == null) {
                config.dsl().newRecord(
                    Tables.USERS,
                    thing,
                )
            } else {
                val existing = getRecord(config.dsl(), thingId)
                if (existing != null) {
                    // don't allow overwriting authCrypt or admin with generic save()
                    existing.from(
                        thing,
                        *existing.fields().filter { it != Tables.USERS.AUTH_CRYPT && it != Tables.USERS.ADMIN }
                            .toTypedArray(),
                    )
                    existing
                } else {
                    config.dsl().newRecord(
                        Tables.USERS,
                        thing,
                    )
                }
            }
            record.store()
            record.into(User::class.java)
        }
    }

    @Timed
    override fun create(user: User, inviteCode: UUID, passwordHash: String): User? {
        return ctx.transactionResult { config ->
            val instanceId =
                config.dsl().select(USER_INVITE.INSTANCE_ID)
                    .from(USER_INVITE)
                    .where(USER_INVITE.ID.eq(inviteCode).and(USER_INVITE.STATUS.eq("ACTIVE")))
                    .fetchOneInto(UUID::class.java)
            if (instanceId != null) {
                val record = config.dsl().newRecord(
                    Tables.USERS,
                    user.copy(id = null, instanceId = instanceId, authCrypt = passwordHash, admin = false),
                )
                record.store()
                record.into(User::class.java)
            } else {
                null
            }
        }
    }

    @Timed
    override fun all(): List<User> {
        return ctx.select().from(Tables.USERS).orderBy(DSL.lower(Tables.USERS.NAME)).fetchInto(User::class.java)
    }

    @Timed
    override fun stream(): Stream<User> {
        return ctx.select().from(Tables.USERS).orderBy(DSL.lower(Tables.USERS.NAME))
            .fetchStreamInto(User::class.java)
    }
}
