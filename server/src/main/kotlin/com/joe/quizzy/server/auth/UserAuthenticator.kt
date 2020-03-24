package com.joe.quizzy.server.auth

import com.joe.quizzy.persistence.api.UserDAO
import io.dropwizard.auth.Authenticator
import io.dropwizard.auth.basic.BasicCredentials
import java.util.Optional
import javax.inject.Inject

class UserAuthenticator
@Inject constructor(
    val userDAO: UserDAO,
    val hasher: Hasher
) : Authenticator<BasicCredentials, UserPrincipal> {
    override fun authenticate(credentials: BasicCredentials): Optional<UserPrincipal> {
        val user = userDAO.getByEmail(credentials.username)
        val userCrypt = user?.authCrypt
        if (userCrypt != null) {
            if (hasher.verify(userCrypt, credentials.password)) {
                return Optional.of(UserPrincipal(user))
            }
        }
        return Optional.empty()
    }
}
