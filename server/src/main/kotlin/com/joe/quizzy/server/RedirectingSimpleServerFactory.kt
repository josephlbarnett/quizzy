package com.joe.quizzy.server

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.jetty.ContextRoutingHandler
import io.dropwizard.server.SimpleServerFactory
import io.dropwizard.setup.Environment
import io.dropwizard.util.Maps
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler

private class RedirectHandler(val destination: String) : AbstractHandler() {
    override fun handle(
        target: String,
        baseRequest: Request,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        response.sendRedirect(destination)
    }
}

@JsonTypeName("simple-redirect")
class RedirectingSimpleServerFactory : SimpleServerFactory() {
    override fun build(environment: Environment): Server {
        // ensures that the environment is configured before the server is built

        // ensures that the environment is configured before the server is built
        configure(environment)

        printBanner(environment.name)
        val threadPool = createThreadPool(environment.metrics())
        val server = buildServer(environment.lifecycle(), threadPool)

        val applicationHandler = createAppServlet(
            server,
            environment.jersey(),
            environment.objectMapper,
            environment.validator,
            environment.applicationContext,
            environment.jerseyServletContainer,
            environment.metrics()
        )

        val adminHandler = createAdminServlet(
            server,
            environment.adminContext,
            environment.metrics(),
            environment.healthChecks()
        )

        val conn = connector.build(
            server,
            environment.metrics(),
            environment.name,
            null
        )

        server.addConnector(conn)

        val handlers =
            Maps.of(
                applicationContextPath, applicationHandler,
                adminContextPath, adminHandler,
                "/", RedirectHandler(applicationContextPath)
            )
        val routingHandler = ContextRoutingHandler(handlers)
        val gzipHandler = buildGzipHandler(routingHandler)
        server.handler = addStatsHandler(addRequestLog(server, gzipHandler, environment.name))

        return server
    }
}
