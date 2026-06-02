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
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import xyz.malefic.kanman.data.BoardEntity
import xyz.malefic.kanman.data.BoardSummaryListModel
import xyz.malefic.kanman.data.Boards
import xyz.malefic.kanman.data.Visibility.PRIVATE
import xyz.malefic.kanman.data.Visibility.PUBLIC
import xyz.malefic.kanman.data.boardLens
import xyz.malefic.kanman.data.boardSummaryListLens
import xyz.malefic.kanman.data.toSummaryModel
import xyz.malefic.kanman.data.transaction.currentUser
import xyz.malefic.kanman.util.auth
import xyz.malefic.kanman.util.error
import xyz.malefic.kanman.util.toVisibility
import kotlin.uuid.Uuid

val get =
    arrayOf(
        "/api/ping" bind GET to { Response(OK).body("pong") },
        "/api/health" bind GET to { Response(OK).body("healthy") },
        "/api/board/{id}" bind GET to
            auth { user, request ->
                val id =
                    request.path("id")?.let { Uuid.parse(it) }
                        ?: return@auth Response(BAD_REQUEST).with("Invalid board id".error)

                try {
                    Response(OK).with(
                        boardLens of
                            (
                                user.boards.firstOrNull { it.id == id }
                                    ?: return@auth Response(NOT_FOUND).with("Board not found".error)
                            ),
                    )
                } catch (e: Exception) {
                    Response(BAD_REQUEST).with("Failed to retrieve board: $e".error)
                }
            },
        "/api/boards" bind GET to { request ->
            try {
                val visibility = request.query("visibility")?.toVisibility
                val user = currentUser(request)

                val boards =
                    transaction {
                        when (visibility) {
                            PUBLIC -> {
                                BoardEntity
                                    .find { Boards.visibility eq PUBLIC }
                                    .map { it.toSummaryModel() }
                            }

                            PRIVATE -> {
                                if (user == null) return@transaction null
                                BoardEntity
                                    .find { Boards.visibility eq PRIVATE }
                                    .filter { board -> board.users.any { u -> u.id.value == user.id } }
                                    .map { it.toSummaryModel() }
                            }

                            else -> {
                                val public =
                                    BoardEntity
                                        .find { Boards.visibility eq PUBLIC }
                                        .map { it.toSummaryModel() }
                                if (user == null) {
                                    public
                                } else {
                                    val privateVisible =
                                        BoardEntity
                                            .find { Boards.visibility eq PRIVATE }
                                            .filter { board -> board.users.any { u -> u.id.value == user.id } }
                                            .map { it.toSummaryModel() }
                                    (public + privateVisible).distinctBy { it.id }
                                }
                            }
                        }
                    }

                if (visibility == PRIVATE && user == null) {
                    Response(UNAUTHORIZED).with("Authentication required for private boards".error)
                } else {
                    Response(OK).with(boardSummaryListLens of BoardSummaryListModel(boards ?: emptyList()))
                }
            } catch (e: Exception) {
                Response(BAD_REQUEST).with("Failed to list boards: $e".error)
            }
        },
    )
