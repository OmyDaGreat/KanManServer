package xyz.malefic.kanman.http

import org.http4k.core.Method.DELETE
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.routing.path
import xyz.malefic.kanman.data.transaction.deleteBoard
import xyz.malefic.kanman.util.auth
import xyz.malefic.kanman.util.error
import kotlin.uuid.Uuid

val delete =
    arrayOf(
        "/api/board/{id}" bind DELETE to
            auth { user ->
                val id = Uuid.parse(path("id") ?: return@auth Response(BAD_REQUEST).with("Invalid board".error))

                try {
                    if (!deleteBoard(id, user)) {
                        return@auth Response(BAD_REQUEST).with("Invalid board".error)
                    }
                } catch (e: Exception) {
                    return@auth Response(INTERNAL_SERVER_ERROR).with("Failed to create board: $e".error)
                }

                Response(OK)
            },
    )
