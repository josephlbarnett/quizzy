package com.joe.quizzy.graphql.auth

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.testng.annotations.Test

class Argon2HasherTest {
    @Test
    fun testAuthenticator() {
        val hasher = Argon2Hasher()
        val hash = hasher.hash("supersecret")
        assertThat(hasher.verify(hash, "supersecret")).isTrue()
        assertThat(hasher.verify(hash, "supersecre2t")).isFalse()
        println(hash)
    }
}
