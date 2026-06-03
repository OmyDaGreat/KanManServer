package xyz.malefic.kanman.util

import at.favre.lib.crypto.bcrypt.BCrypt
import org.http4k.core.Filter
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.RequestKey
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import xyz.malefic.kanman.data.transaction.getUserFromAccessToken
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.uuid.Uuid

const val ACCESS_TOKEN_TTL_MILLIS = 15 * 60 * 1000L
const val REFRESH_TOKEN_TTL_MILLIS = 30 * 24 * 60 * 60 * 1000L

private val secureRandom = SecureRandom()
private val bcrypt = BCrypt.withDefaults()
private val verifier = BCrypt.verifyer()

val authenticatedUserId = RequestKey.required<Uuid>("authenticated-user-id")

val auth: Filter =
    Filter { next ->
        { request ->
            val token =
                request
                    .header("Authorization")
                    ?.takeIf { it.startsWith("Bearer ") }
                    ?.removePrefix("Bearer ")
                    ?.trim()
            if (token.isNullOrBlank()) {
                error(UNAUTHORIZED) { "Missing bearer token" }
            } else {
                val user = getUserFromAccessToken(token)
                if (user == null) {
                    error(UNAUTHORIZED) { "Invalid or expired token" }
                } else {
                    next(request.with(authenticatedUserId of user.id))
                }
            }
        }
    }

val authWS: WsFilter =
    WsFilter { next ->
        { request ->
            val token =
                request
                    .header("Authorization")
                    ?.takeIf { it.startsWith("Bearer ") }
                    ?.removePrefix("Bearer ")
                    ?.trim()
            if (token.isNullOrBlank()) {
                WsResponse { ws ->
                    ws.send(WsMessage("Error: Missing bearer token"))
                    ws.close()
                }
            } else {
                val user = getUserFromAccessToken(token)
                if (user == null) {
                    WsResponse { ws ->
                        ws.send(WsMessage("Error: Invalid or expired token"))
                        ws.close()
                    }
                } else {
                    next(request.with(authenticatedUserId of user.id))
                }
            }
        }
    }

fun hashPassword(password: String): String = bcrypt.hashToString(12, password.toCharArray())

fun verifyPassword(
    password: String,
    hashedPassword: String,
): Boolean = verifier.verify(password.toCharArray(), hashedPassword).verified

fun generateToken(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

fun hashToken(token: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
