package com.joe.quizzy.graphql.auth

import com.joe.quizzy.persistence.api.UserDAO
import io.dropwizard.auth.Authenticator
import io.dropwizard.auth.basic.BasicCredentials
import jakarta.inject.Inject
import java.security.Principal
import java.util.Optional

class UserAuthenticator
    @Inject
    constructor(
        private val userDAO: UserDAO,
        val hasher: Hasher,
    ) : Authenticator<BasicCredentials, Principal> {
        override fun authenticate(credentials: BasicCredentials): Optional<Principal> {
            val user = userDAO.getByEmail(credentials.username)
            val userCrypt = user?.authCrypt
            if (userCrypt != null) {
                if (hasher.verify(userCrypt, credentials.password)) {
                    return Optional.of(UserPrincipal(user, null))
                }
            }
            return Optional.empty()
        }
    }
