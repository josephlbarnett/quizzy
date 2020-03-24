package com.joe.quizzy.server.auth

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import de.mkammerer.argon2.Argon2Factory
import org.testng.annotations.Test

class UserAuthenticatorTest {
    @Test
    fun testAuthenticator() {
        val argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
        val hash = argon.hash(100, 1024, 2, "supersecret".toCharArray())
        assertThat(argon.verify(hash, "supersecret".toCharArray())).isTrue()
        assertThat(argon.verify(hash, "supersecre2t".toCharArray())).isFalse()
        println(hash)
    }
}
