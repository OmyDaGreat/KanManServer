package xyz.malefic.kanman.http

import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import xyz.malefic.kanman.corsPolicy

val http: RoutingHttpHandler =
    ServerFilters.Cors(corsPolicy).then(
        routes(
            *get,
            *post,
        ),
    )
