package xyz.malefic.kanman.http

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.routing.bind
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import xyz.malefic.kanman.data.BoardsListModel
import xyz.malefic.kanman.data.UserEntity
import xyz.malefic.kanman.data.Users
import xyz.malefic.kanman.data.boardsListLens
import xyz.malefic.kanman.data.error
import xyz.malefic.kanman.data.errorLens
import xyz.malefic.kanman.data.toModel
import xyz.malefic.kanman.util.auth

val get =
    arrayOf(
        "/api/ping" bind Method.GET to { Response.Companion(Status.OK).body("pong") },
        "/api/health" bind Method.GET to { Response.Companion(Status.OK).body("healthy") },
        "/api/user/boards" bind Method.GET to
            auth REQUEST@{ _, user ->
                try {
                    transaction {
                        UserEntity
                            .find { Users.username eq user.username }
                            .firstOrNull()
                            ?.let { user ->
                                if (user.password != user.password) {
                                    return@transaction Response
                                        .Companion(Status.UNAUTHORIZED)
                                        .with(errorLens of "Invalid password".error)
                                }
                                val boards = user.boards.map { it.toModel() }
                                Response
                                    .Companion(Status.OK)
                                    .with(boardsListLens of BoardsListModel(user.username, boards))
                            }
                            ?: Response.Companion(Status.NOT_FOUND).with(errorLens of "User not found".error)
                    }
                } catch (e: Exception) {
                    Response
                        .Companion(Status.BAD_REQUEST)
                        .with(errorLens of "Failed to retrieve boards: $e".error)
                }
            },
    )
