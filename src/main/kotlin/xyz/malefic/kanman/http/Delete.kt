package xyz.malefic.kanman.http

import org.http4k.core.Method.DELETE
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import xyz.malefic.kanman.data.transaction.deleteBoard
import xyz.malefic.kanman.util.authRequest
import xyz.malefic.kanman.util.error
import xyz.malefic.kanman.util.response
import kotlin.uuid.Uuid

val delete =
    arrayOf(
        "/api/board/{id}" bind DELETE to
            authRequest { user ->
                val id =
                    Uuid.parseOrNull(path("id") ?: return@authRequest error(BAD_REQUEST) { "Invalid board" })
                        ?: return@authRequest error(BAD_REQUEST) { "Invalid board id" }

                try {
                    if (!deleteBoard(id, user)) {
                        return@authRequest error(BAD_REQUEST) { "Invalid board" }
                    }
                } catch (e: Exception) {
                    return@authRequest error(INTERNAL_SERVER_ERROR) { "Failed to create board: $e" }
                }

                response(OK)
            },
    )
