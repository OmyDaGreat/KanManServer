package xyz.malefic.kanman.util

import co.touchlab.kermit.Logger
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import xyz.malefic.kanman.data.ErrorModel
import xyz.malefic.kanman.data.UserResponseModel
import xyz.malefic.kanman.data.transaction.currentUser

fun catch(
    message: String,
    func: HttpHandler,
) = { request: Request ->
    try {
        func(request)
    } catch (e: Exception) {
        Logger.e(e, "HTTP") { message }
        error(INTERNAL_SERVER_ERROR) { message }
    }
}

fun catchPlus(
    message: String,
    func: () -> HttpHandler,
) = catch(message, func())

inline fun <reified A : Any> model(crossinline handler: (Request, A) -> Response): HttpHandler =
    request@{ request ->
        val a =
            try {
                lens<A>()(request)
            } catch (e: Exception) {
                return@request error(BAD_REQUEST) { "Invalid JSON for request body: $e" }
            }
        handler(request, a)
    }

fun authRequest(next: Request.(UserResponseModel) -> Response) =
    auth.then { request ->
        request.next(
            currentUser(request) ?: return@then error(UNAUTHORIZED) { "Authenticated user not found" },
        )
    }

inline fun <reified T : Any> authModel(crossinline next: (UserResponseModel, T) -> Response) =
    auth.then(
        model<T> { request, lensRequest ->
            val user = currentUser(request) ?: return@model error(UNAUTHORIZED) { "Authenticated user not found" }
            next(user, lensRequest)
        },
    )

inline fun <reified T : Any> lens() = Body.auto<T>().toLens()

inline fun <reified T : Any> value(obj: T) = lens<T>().of<Response>(obj)

fun response(
    status: Status,
    modifier: (Response) -> Response = { it },
) = Response(status).with(modifier)

fun error(
    status: Status,
    message: () -> String,
) = response(status) { value(ErrorModel(message()))(it) }
