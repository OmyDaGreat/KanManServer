package xyz.malefic.kanman

import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.AllowAllOriginPolicy
import org.http4k.filter.CorsPolicy
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer

fun createApp(): HttpHandler {
    val corsPolicy =
        CorsPolicy(
            headers = listOf("Content-Type"),
            methods = listOf(GET, POST, PUT, DELETE),
            originPolicy = AllowAllOriginPolicy,
        )

    val corsFilter = ServerFilters.Cors(corsPolicy)

    return corsFilter.then(
        routes(
            "/api/ping" bind GET to { Response(OK).body("pong") },
            "/api/health" bind GET to { Response(OK).body("healthy") },
            "/api/board/{id}" bind GET to { request ->
                val id = request.query("id")
                Response(OK).body("id: $id") // TODO: Return board contents // TODO: Figure out auth w/ visibility
            },
        ),
    )
}

val app: HttpHandler by lazy { createApp() }

fun main() {
    val printingApp: HttpHandler = PrintRequest().then(app)

    val server = printingApp.asServer(Undertow(6320)).start()

    println("Server started on port ${server.port()}!")
}
