package xyz.malefic.kanman

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import xyz.malefic.kanman.data.BoardEntity
import xyz.malefic.kanman.data.BoardsListModel
import xyz.malefic.kanman.data.UserEntity
import xyz.malefic.kanman.data.Users
import xyz.malefic.kanman.data.Visibility
import xyz.malefic.kanman.data.boardLens
import xyz.malefic.kanman.data.boardsListLens
import xyz.malefic.kanman.data.error
import xyz.malefic.kanman.data.errorLens
import xyz.malefic.kanman.data.toModel
import xyz.malefic.kanman.data.userLens
import xyz.malefic.kanman.util.auth

val http: RoutingHttpHandler =
    ServerFilters.Cors(corsPolicy).then(
        routes(
            "/api/ping" bind GET to { Response(OK).body("pong") },
            "/api/health" bind GET to { Response(OK).body("healthy") },
            "/api/user/register" bind POST to
                auth REQUEST@{ _, user ->
                    // TODO: More proper user creds validation (+ password hashing)
                    val userResult =
                        try {
                            transaction {
                                UserEntity.new {
                                    this.username = user.username
                                    this.password = user.password
                                }
                            }
                        } catch (e: Exception) {
                            return@REQUEST Response(BAD_REQUEST).with(errorLens of "Failed to create user: $e".error)
                        }

                    Response(OK).with(userLens of userResult.toModel())
                },
            "/api/user/boards" bind GET to
                auth REQUEST@{ _, user ->
                    try {
                        transaction {
                            UserEntity
                                .find { Users.username eq user.username }
                                .firstOrNull()
                                ?.let { user ->
                                    if (user.password != user.password) {
                                        return@transaction Response(UNAUTHORIZED).with(errorLens of "Invalid password".error)
                                    }
                                    val boards = user.boards.map { it.toModel() }
                                    Response(OK).with(boardsListLens of BoardsListModel(user.username, boards))
                                }
                                ?: Response(NOT_FOUND).with(errorLens of "User not found".error)
                        }
                    } catch (e: Exception) {
                        Response(BAD_REQUEST).with(errorLens of "Failed to retrieve boards: $e".error)
                    }
                },
            "/api/board/create" bind POST to REQUEST@{ request ->
                val title =
                    request.query("title")
                        ?: return@REQUEST Response(BAD_REQUEST).with(errorLens of "Expected title, instead got nothing".error)
                val visibilityStr =
                    request.query("visibility")
                        ?: return@REQUEST Response(
                            BAD_REQUEST,
                        ).with(errorLens of "Expected visibility (PUBLIC/PRIVATE), instead got nothing".error)
                val visibility =
                    try {
                        Visibility.valueOf(visibilityStr.uppercase())
                    } catch (_: IllegalArgumentException) {
                        return@REQUEST Response(BAD_REQUEST)
                            .with(errorLens of "Invalid visibility '$visibilityStr'. Expected PUBLIC or PRIVATE.".error)
                    }

                val board =
                    try {
                        transaction {
                            BoardEntity.new {
                                this.title = title
                                this.visibility = visibility
                            }
                        }
                    } catch (e: Exception) {
                        return@REQUEST Response(BAD_REQUEST).with(errorLens of "Failed to create board: $e".error)
                    }

                Response(OK).with(boardLens of board.toModel())
            },
        ),
    )
