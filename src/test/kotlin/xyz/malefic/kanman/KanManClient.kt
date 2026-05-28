package xyz.malefic.kanman

import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintResponse

fun main() {
    val client: HttpHandler = OkHttp()

    val printingClient: HttpHandler = PrintResponse().then(client)

    val response: Response = printingClient(Request(GET, "http://localhost:6320/api/ping"))

    println(response.bodyString())
}
