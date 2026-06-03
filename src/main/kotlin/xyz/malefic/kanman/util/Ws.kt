package xyz.malefic.kanman.util

import org.http4k.core.Request
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.then
import xyz.malefic.kanman.data.ErrorModel
import xyz.malefic.kanman.data.UserResponseModel
import xyz.malefic.kanman.data.transaction.currentUser

fun authWS(next: (UserResponseModel, Request) -> WsResponse) =
    authWS.then { request ->
        next(
            currentUser(request) ?: return@then WsResponse { ws ->
                ws.send(WsMessage("Error: Authenticated user not found"))
                ws.close()
            },
            request,
        )
    }

class WsAbort(
    override val message: String,
    override val cause: Exception? = null,
) : Exception(message, cause)

fun abortWS(
    message: String,
    cause: Exception? = null,
): Nothing = throw WsAbort(message, cause)

inline fun <reified T : Any> wsLens(msg: WsMessage) = WsMessage.auto<T>().toLens()(msg)

inline fun <reified T : Any> wsLens(obj: T) = WsMessage.auto<T>().toLens()(obj)

inline fun <reified T : Any> wsValue(obj: T) = wsLens(obj)

fun Websocket.error(message: String) = send(wsValue(ErrorModel(message)))
