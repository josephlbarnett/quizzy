package com.joe.quizzy.graphql.mail

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.Gmail.Users.Messages.Send
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.Oauth2Scopes
import com.google.common.io.BaseEncoding
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.joe.quizzy.persistence.api.InstanceDAO
import dev.misfitlabs.kotlinguice4.KotlinModule
import jakarta.inject.Inject
import jakarta.mail.internet.MimeMessage
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.util.UUID

private val log = KotlinLogging.logger {}

open class GmailService
    @Inject
    constructor(
        jsonFactory: JsonFactory,
        dataStoreFactory: DataStoreFactory,
        @Assisted instanceId: UUID,
    ) {
        open lateinit var gmail: Gmail
        open lateinit var oauth: Oauth2

        init {
            val envSecrets = System.getenv("GMAIL_CREDENTIALS_JSON")
            val secretsReader =
                if (envSecrets.isNullOrBlank()) {
                    this::class.java.getResourceAsStream("/gmail-credentials.json")?.let { InputStreamReader(it) }
                } else {
                    StringReader(envSecrets)
                }
            val secrets =
                secretsReader?.use { reader ->
                    GoogleClientSecrets.load(jsonFactory, reader)
                }
            val credential =
                AuthorizationCodeInstalledApp(
                    GoogleAuthorizationCodeFlow
                        .Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            jsonFactory,
                            secrets,
                            listOf(GmailScopes.GMAIL_SEND, Oauth2Scopes.USERINFO_EMAIL),
                        ).setDataStoreFactory(dataStoreFactory)
                        .setAccessType("offline")
                        .build(),
//            LocalServerReceiver.Builder().setPort(8888)
//                .build() // < --to allow for setting refresh token w / localhost server
                    object : VerificationCodeReceiver { // <-- don't get new tokens
                        override fun waitForCode(): String =
                            throw IOException("Can't wait for code, need a refresh token persisted")

                        override fun stop() {
                            // do nothing
                        }

                        override fun getRedirectUri(): String =
                            throw IOException("No redirect URI, need a refresh token persisted")
                    },
                ).authorize(instanceId.toString())

            gmail =
                Gmail
                    .Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        jsonFactory,
                        credential,
                    ).setApplicationName("rules exchange")
                    .build()

            oauth =
                Oauth2
                    .Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        jsonFactory,
                        credential,
                    ).setApplicationName("rules exchange")
                    .build()
        }
    }

interface InternalGmailMailServiceFactory {
    fun create(instanceId: UUID): GmailService
}

open class GmailServiceFactory
    @Inject
    constructor(
        private val factory: InternalGmailMailServiceFactory,
        private val dataStoreFactory: DataStoreFactory,
        private val instanceDAO: InstanceDAO,
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
        bind<JsonFactory>().to<GsonFactory>()
        install(FactoryModuleBuilder().build(InternalGmailMailServiceFactory::class.java))
    }
}

/**
 * Convenience extension method to send a [MimeMessage] instead of a [Message]
 */
fun Gmail.sendEmail(
    userId: String,
    message: MimeMessage,
): Send {
    val buffer = ByteArrayOutputStream()
    message.writeTo(buffer)
    val encodedEmail = BaseEncoding.base64Url().omitPadding().encode(buffer.toByteArray())
    val messageToSend = Message()
    messageToSend.raw = encodedEmail
    return this.users().messages().send(userId, messageToSend)
}
