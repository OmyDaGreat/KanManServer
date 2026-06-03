package xyz.malefic.kanman.http

import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.routing.bind
import xyz.malefic.kanman.data.BoardCreateModel
import xyz.malefic.kanman.data.RefreshRequestModel
import xyz.malefic.kanman.data.UserRequestModel
import xyz.malefic.kanman.data.transaction.createBoard
import xyz.malefic.kanman.data.transaction.createUser
import xyz.malefic.kanman.data.transaction.getTokensFromLogin
import xyz.malefic.kanman.data.transaction.refreshTokens
import xyz.malefic.kanman.util.authModel
import xyz.malefic.kanman.util.catchPlus
import xyz.malefic.kanman.util.error
import xyz.malefic.kanman.util.model
import xyz.malefic.kanman.util.response
import xyz.malefic.kanman.util.value

val post =
    arrayOf(
        "/api/login" bind POST to
            catchPlus("Failed to process login") {
                model<UserRequestModel> { _, login ->
                    val tokens = getTokensFromLogin(login) ?: return@model error(UNAUTHORIZED) { "Invalid username or password" }

                    response(OK, value(tokens))
                }
            },
        "/api/token/refresh" bind POST to
            catchPlus("Failed to refresh tokens") {
                model<RefreshRequestModel> { _, refresh ->
                    val tokens =
                        refreshTokens(refresh.refreshToken) ?: return@model error(UNAUTHORIZED) { "Invalid or expired refresh token" }

                    response(OK, value(tokens))
                }
            },
        "/api/user/register" bind POST to
            catchPlus("Failed to register user") {
                model<UserRequestModel> { _, user ->
                    val userResult = createUser(user)

                    response(OK, value(userResult))
                }
            },
        "/api/board" bind POST to
            catchPlus("Failed to create board") {
                authModel<BoardCreateModel> { user, boardRequest ->
                    val boardResponse = createBoard(boardRequest, user)

                    response(OK, value(boardResponse))
                }
            },
    )
