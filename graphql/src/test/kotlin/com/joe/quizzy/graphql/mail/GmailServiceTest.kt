package com.joe.quizzy.graphql.mail

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStoreFactory
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import com.trib3.testing.LeakyMock
import dev.misfitlabs.kotlinguice4.KotlinModule
import jakarta.inject.Inject
import org.easymock.EasyMock
import org.testng.annotations.Guice
import org.testng.annotations.Test
import java.util.UUID

val persistedUUID = UUID.randomUUID()
val persistedInstance = Instance(persistedUUID, "persisted", "ACtIVE", persistedUUID.toString())

class GmailServiceTestModule : KotlinModule() {
    override fun configure() {
        val mockDAO = LeakyMock.niceMock<InstanceDAO>()
        EasyMock.expect(mockDAO.get(persistedUUID)).andReturn(persistedInstance)
        EasyMock.replay(mockDAO)
        bind<InstanceDAO>().toInstance(mockDAO)
    }
}

@Guice(modules = [GmailServiceModule::class, GmailServiceTestModule::class])
class GmailServiceTest @Inject constructor(
    val factory: GmailServiceFactory,
    dataStoreFactory: DataStoreFactory,
) {
    val goodUUID = UUID.randomUUID()

    init {
        val credStore = StoredCredential.getDefaultDataStore(dataStoreFactory)
        credStore.set(goodUUID.toString(), StoredCredential().apply { refreshToken = goodUUID.toString() })
    }

    @Test
    fun testGmail() {
        assertThat(factory.getService(UUID.randomUUID())).isNull()
        val service = factory.getService(goodUUID)
        assertThat(service).isNotNull()
        assertThat(service?.gmail).isNotNull()
        assertThat(service?.oauth).isNotNull()
        val persistedService = factory.getService(persistedUUID)
        assertThat(persistedService).isNotNull()
        assertThat(persistedService?.gmail).isNotNull()
        assertThat(persistedService?.oauth).isNotNull()
    }
}
