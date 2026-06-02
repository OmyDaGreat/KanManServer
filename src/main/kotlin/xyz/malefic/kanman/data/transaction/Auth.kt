package xyz.malefic.kanman.data.transaction

import org.http4k.core.Request
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import xyz.malefic.kanman.data.AuthTokenEntity
import xyz.malefic.kanman.data.AuthTokens
import xyz.malefic.kanman.data.TokenResponseModel
import xyz.malefic.kanman.data.TokenType
import xyz.malefic.kanman.data.UserEntity
import xyz.malefic.kanman.data.UserRequestModel
import xyz.malefic.kanman.data.Users
import xyz.malefic.kanman.data.toResponseModel
import xyz.malefic.kanman.util.ACCESS_TOKEN_TTL_MILLIS
import xyz.malefic.kanman.util.REFRESH_TOKEN_TTL_MILLIS
import xyz.malefic.kanman.util.authenticatedUserId
import xyz.malefic.kanman.util.generateToken
import xyz.malefic.kanman.util.hashToken
import xyz.malefic.kanman.util.nowMs
import xyz.malefic.kanman.util.verifyPassword

fun currentUser(request: Request) =
    transaction {
        UserEntity.findById(authenticatedUserId(request))?.toResponseModel()
    }

fun getTokensFromLogin(user: UserRequestModel) =
    transaction {
        UserEntity
            .find { Users.username eq user.username }
            .firstOrNull()
            ?.takeIf { verifyPassword(user.password, it.hashedPassword) }
            ?.let { issueTokenPair(it) }
    }

fun refreshTokens(refreshToken: String) =
    transaction {
        val token = findValidToken(refreshToken, TokenType.REFRESH) ?: return@transaction null
        token.revokedAt = nowMs()
        issueTokenPair(token.user)
    }

@Suppress("UnusedReceiverParameter")
fun JdbcTransaction.issueTokenPair(user: UserEntity): TokenResponseModel {
    val accessToken = generateToken()
    val refreshToken = generateToken()
    val accessExpiration = nowMs() + ACCESS_TOKEN_TTL_MILLIS
    val refreshExpiration = nowMs() + REFRESH_TOKEN_TTL_MILLIS

    AuthTokenEntity.new {
        this.user = user
        tokenType = TokenType.ACCESS
        tokenHash = hashToken(accessToken)
        expiresAt = accessExpiration
        revokedAt = null
    }

    AuthTokenEntity.new {
        this.user = user
        tokenType = TokenType.REFRESH
        tokenHash = hashToken(refreshToken)
        expiresAt = refreshExpiration
        revokedAt = null
    }

    return TokenResponseModel(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = ACCESS_TOKEN_TTL_MILLIS / 1000,
    )
}

@Suppress("UnusedReceiverParameter")
fun JdbcTransaction.findValidToken(
    token: String,
    kind: TokenType,
) = AuthTokenEntity
    .find { AuthTokens.tokenHash eq hashToken(token) }
    .firstOrNull()
    ?.takeIf { it.tokenType == kind && it.revokedAt == null && it.expiresAt > nowMs() }
