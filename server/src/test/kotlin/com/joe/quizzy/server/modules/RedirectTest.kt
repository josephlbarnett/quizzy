package com.joe.quizzy.server.modules

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import com.trib3.server.modules.ServletConfig
import com.trib3.server.modules.TribeApplicationModule
import com.trib3.testing.LeakyMock
import com.trib3.testing.server.ResourceTestBase
import org.easymock.EasyMock
import org.glassfish.jersey.client.ClientProperties.FOLLOW_REDIRECTS
import org.testng.annotations.Guice
import org.testng.annotations.Test
import javax.inject.Inject
import javax.inject.Named
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Guice(modules = [QuizzyServiceModule::class])
class RedirectTest @Inject constructor(
    @Named(TribeApplicationModule.APPLICATION_SERVLETS_BIND_NAME)
    val appServlets: Set<@JvmSuppressWildcards ServletConfig>
) : ResourceTestBase<RedirectResource>() {
    override fun getResource(): RedirectResource {
        return RedirectResource()
    }

    @Test
    fun testAssetsRedirect() {
        val response = resource.target("/").property(FOLLOW_REDIRECTS, false)
            .request()
            .get()
        assertThat(response.status).isEqualTo(302)
        assertThat(response.location.toString()).endsWith("/app/assets")
    }

    @Test
    fun testAppRedirect() {
        val servletConfig = appServlets.filter { it.name == "root-redirect" }.first()
        val req = LeakyMock.mock<HttpServletRequest>()
        val res = LeakyMock.mock<HttpServletResponse>()
        EasyMock.expect(req.method).andReturn("GET")
        EasyMock.expect(res.sendRedirect("/app/")).once()
        EasyMock.replay(req, res)
        servletConfig.servlet.service(req, res)
        EasyMock.verify(req, res)
    }
}
