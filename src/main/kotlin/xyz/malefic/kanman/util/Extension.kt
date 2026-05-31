package xyz.malefic.kanman.util

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.with
import xyz.malefic.kanman.data.LoginModel
import xyz.malefic.kanman.data.error
import xyz.malefic.kanman.data.errorLens
import xyz.malefic.kanman.data.loginLens

fun auth(handler: (Request, LoginModel) -> Response): HttpHandler =
    REQUEST@{ request ->
        val userOrError =
            try {
                loginLens(request)
            } catch (e: Exception) {
                return@REQUEST Response(BAD_REQUEST)
                    .with(errorLens of "Invalid JSON for authentication: $e".error)
            }
        handler(request, userOrError)
    }
