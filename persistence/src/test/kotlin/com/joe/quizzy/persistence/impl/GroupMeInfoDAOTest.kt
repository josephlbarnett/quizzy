package com.joe.quizzy.persistence.impl

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.GroupMeInfo
import com.joe.quizzy.persistence.api.GroupMeInfoDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import org.jooq.Configuration
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.UUID

class GroupMeInfoDAOTest : PostgresDAOTestBase() {
    lateinit var dao: GroupMeInfoDAO
    lateinit var instanceDAO: InstanceDAO

    @BeforeClass
    override fun setUp() {
        super.setUp()
        dao = GroupMeInfoDAOJooq(ctx)
        instanceDAO = InstanceDAOJooq(ctx)
    }

    @Test
    fun testGroupMeInfo() {
        val instance = instanceDAO.save(Instance(null, "groupmedaoinstance", "ACTIVE"))
        ctx.transaction { config: Configuration ->
            config.dsl().insertInto(Tables.GROUPME_INFO)
                .columns(Tables.GROUPME_INFO.INSTANCE_ID, Tables.GROUPME_INFO.GROUP_ID, Tables.GROUPME_INFO.API_KEY)
                .values(instance.id, "groupId", "apiKey").execute()
        }


        assertThat(dao.get(UUID.randomUUID())).isNull()
        assertThat(dao.get(instance.id!!)).isNotNull().all {
            prop(GroupMeInfo::groupId).isEqualTo("groupId")
            prop(GroupMeInfo::apiKey).isEqualTo("apiKey")
        }
    }

}
