package com.joe.quizzy.server.modules

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import com.trib3.server.modules.ServletConfig
import com.trib3.server.modules.TribeApplicationModule
import com.trib3.testing.LeakyMock
import com.trib3.testing.server.ResourceTestBase
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.easymock.EasyMock
import org.glassfish.jersey.client.ClientProperties.FOLLOW_REDIRECTS
import org.testng.annotations.Guice
import org.testng.annotations.Test

@Guice(modules = [QuizzyServiceModule::class])
class RedirectTest
    @Inject
    constructor(
        @Named(TribeApplicationModule.APPLICATION_SERVLETS_BIND_NAME)
        val appServlets: Set<ServletConfig>,
    ) : ResourceTestBase<RedirectResource>() {
        override fun getResource(): RedirectResource {
            return RedirectResource()
        }

        @Test
        fun testAssetsRedirect() {
            val response =
                resource.target("/").property(FOLLOW_REDIRECTS, false)
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

        @Test
        fun testHttpsRedirectToHttps() {
            val filter = HttpsFilter()
            val req = LeakyMock.mock<HttpServletRequest>()
            val res = LeakyMock.mock<HttpServletResponse>()
            val chain = LeakyMock.mock<FilterChain>()
            EasyMock.expect(req.getHeader("X-Forwarded-Proto")).andReturn("http").atLeastOnce()
            EasyMock.expect(req.serverName).andReturn("blahblah").atLeastOnce()
            EasyMock.expect(req.requestURI).andReturn("/deblah").atLeastOnce()
            EasyMock.expect(res.sendRedirect("https://blahblah/deblah"))
            EasyMock.replay(req, res, chain)
            filter.doFilter(req, res, chain)
            EasyMock.verify(req, res, chain)
        }

        @Test
        fun testHttpsRedirectToHttpsRoot() {
            val filter = HttpsFilter()
            val req = LeakyMock.mock<HttpServletRequest>()
            val res = LeakyMock.mock<HttpServletResponse>()
            val chain = LeakyMock.mock<FilterChain>()
            EasyMock.expect(req.getHeader("X-Forwarded-Proto")).andReturn("http").atLeastOnce()
            EasyMock.expect(req.serverName).andReturn("blahblah").atLeastOnce()
            EasyMock.expect(req.requestURI).andReturn(null).atLeastOnce()
            EasyMock.expect(res.sendRedirect("https://blahblah"))
            EasyMock.replay(req, res, chain)
            filter.doFilter(req, res, chain)
            EasyMock.verify(req, res, chain)
        }

        @Test
        fun testHttpsRedirectLocalhost() {
            val filter = HttpsFilter()
            val req = LeakyMock.mock<HttpServletRequest>()
            val res = LeakyMock.mock<HttpServletResponse>()
            val chain = LeakyMock.mock<FilterChain>()
            EasyMock.expect(req.getHeader("X-Forwarded-Proto")).andReturn(null).atLeastOnce()
            EasyMock.expect(chain.doFilter(req, res))
            EasyMock.replay(req, res, chain)
            filter.doFilter(req, res, chain)
            EasyMock.verify(req, res, chain)
        }

        @Test
        fun testHttpsRedirectCantInspectReq() {
            val filter = HttpsFilter()
            val req = LeakyMock.mock<ServletRequest>()
            val res = LeakyMock.mock<HttpServletResponse>()
            val chain = LeakyMock.mock<FilterChain>()
            EasyMock.expect(chain.doFilter(req, res))
            EasyMock.replay(req, res, chain)
            filter.doFilter(req, res, chain)
            EasyMock.verify(req, res, chain)
        }

        @Test
        fun testHttpsRedirectCantInspectRes() {
            val filter = HttpsFilter()
            val req = LeakyMock.mock<HttpServletRequest>()
            val res = LeakyMock.mock<ServletResponse>()
            val chain = LeakyMock.mock<FilterChain>()
            EasyMock.expect(chain.doFilter(req, res))
            EasyMock.replay(req, res, chain)
            filter.doFilter(req, res, chain)
            EasyMock.verify(req, res, chain)
        }

        @Test
        fun testHttpsRedirectAlreadyHttps() {
            val filter = HttpsFilter()
            val req = LeakyMock.mock<HttpServletRequest>()
            val res = LeakyMock.mock<HttpServletResponse>()
            val chain = LeakyMock.mock<FilterChain>()
            EasyMock.expect(req.getHeader("X-Forwarded-Proto")).andReturn("https").atLeastOnce()
            EasyMock.expect(chain.doFilter(req, res))
            EasyMock.replay(req, res, chain)
            filter.doFilter(req, res, chain)
            EasyMock.verify(req, res, chain)
        }
    }
