package com.joe.quizzy.server.mail

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.Base64
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.Gmail.Users.Messages.Send
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.Oauth2Scopes
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.joe.quizzy.persistence.api.InstanceDAO
import dev.misfitlabs.kotlinguice4.KotlinModule
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.util.UUID
import javax.inject.Inject
import javax.mail.internet.MimeMessage

private val log = KotlinLogging.logger {}

open class GmailService @Inject constructor(
    jacksonFactory: JacksonFactory,
    dataStoreFactory: DataStoreFactory,
    @Assisted instanceId: UUID
) {
    open val gmail: Gmail
    open val oauth: Oauth2

    init {
        val envSecrets = System.getenv("GMAIL_CREDENTIALS_JSON")
        val secretsReader = if (envSecrets.isNullOrBlank()) {
            InputStreamReader(this::class.java.getResourceAsStream("/gmail-credentials.json"))
        } else {
            StringReader(envSecrets)
        }
        val secrets = secretsReader.use { reader ->
            GoogleClientSecrets.load(jacksonFactory, reader)
        }
        val credential = AuthorizationCodeInstalledApp(
            GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                jacksonFactory,
                secrets,
                listOf(GmailScopes.GMAIL_SEND, Oauth2Scopes.USERINFO_EMAIL)
            )
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build(),
//            LocalServerReceiver.Builder().setPort(8888)
//                .build() // < --to allow for setting refresh token w / localhost server
            object : VerificationCodeReceiver { // <-- don't get new tokens
                override fun waitForCode(): String {
                    throw IOException("Can't wait for code, need a refresh token persisted")
                }

                override fun stop() {
                }

                override fun getRedirectUri(): String {
                    throw IOException("No redirect URI, need a refresh token persisted")
                }
            }
        ).authorize(instanceId.toString())

        gmail = Gmail.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("rules exchange").build()

        oauth = Oauth2.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("rules exchange").build()
    }
}

interface InternalGmailMailServiceFactory {
    fun create(instanceId: UUID): GmailService
}

open class GmailServiceFactory @Inject constructor(
    private val factory: InternalGmailMailServiceFactory,
    private val dataStoreFactory: DataStoreFactory,
    private val instanceDAO: InstanceDAO
) {
    open fun getService(instanceId: UUID): GmailService? {
        val credStore = StoredCredential.getDefaultDataStore(dataStoreFactory)
        val instance = instanceDAO.get(instanceId)
        val existingToken = instance?.gmailRefreshToken
        if (existingToken != null) {
            credStore.set(instanceId.toString(), StoredCredential().apply { refreshToken = existingToken })
        }
        return try {
            factory.create(instanceId)
        } catch (e: Exception) {
            log.warn("Could not create GmailSender for instance $instanceId", e)
            null
        } finally {
            val refreshedRefreshToken = credStore.get(instanceId.toString())?.refreshToken
            if (instance != null &&
                refreshedRefreshToken != null &&
                existingToken != refreshedRefreshToken
            ) {
                instanceDAO.save(instance.copy(gmailRefreshToken = refreshedRefreshToken))
            }
        }
    }
}

class GmailServiceModule : KotlinModule() {
    override fun configure() {
        bind<DataStoreFactory>().to<MemoryDataStoreFactory>().asEagerSingleton()
        install(FactoryModuleBuilder().build(InternalGmailMailServiceFactory::class.java))
    }
}

/**
 * Convenience extension method to send a [MimeMessage] instead of a [Message]
 */
fun Gmail.sendEmail(userId: String, message: MimeMessage): Send {
    val buffer = ByteArrayOutputStream()
    message.writeTo(buffer)
    val encodedEmail = Base64.encodeBase64URLSafeString(buffer.toByteArray())
    val messageToSend = Message()
    messageToSend.raw = encodedEmail
    return this.users().messages().send(userId, messageToSend)
}
