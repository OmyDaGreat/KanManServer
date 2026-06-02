package xyz.malefic.kanman.http

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.routing.path
import xyz.malefic.kanman.data.BoardSummaryListModel
import xyz.malefic.kanman.data.boardLens
import xyz.malefic.kanman.data.boardSummaryListLens
import xyz.malefic.kanman.data.transaction.currentUser
import xyz.malefic.kanman.data.transaction.getUserBoards
import xyz.malefic.kanman.util.auth
import xyz.malefic.kanman.util.catch
import xyz.malefic.kanman.util.catchPlus
import xyz.malefic.kanman.util.error
import xyz.malefic.kanman.util.toVisibility
import kotlin.uuid.Uuid

val get =
    arrayOf(
        "/api/ping" bind GET to { Response(OK).body("pong") },
        "/api/health" bind GET to { Response(OK).body("healthy") },
        "/api/board/{id}" bind GET to
            catchPlus("Failed to retrieve board") {
                auth { user, request ->
                    val id = request.path("id")?.let { Uuid.parse(it) } ?: return@auth Response(BAD_REQUEST).with("Invalid board id".error)
                    val board = user.boards.firstOrNull { it.id == id } ?: return@auth Response(NOT_FOUND).with("Board not found".error)

                    Response(OK).with(boardLens of board)
                }
            },
        "/api/boards" bind GET to
            catch("Failed to list boards") { request ->
                val visibility = request.query("visibility")?.toVisibility
                val user = currentUser(request)
                val boards =
                    getUserBoards(visibility, user)
                        ?: run { return@catch Response(UNAUTHORIZED).with("Authentication required for private boards".error) }

                Response(OK).with(boardSummaryListLens of BoardSummaryListModel(boards))
            },
    )
