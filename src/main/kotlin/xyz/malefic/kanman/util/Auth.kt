package xyz.malefic.kanman.util

import at.favre.lib.crypto.bcrypt.BCrypt
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.RequestKey
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import xyz.malefic.kanman.data.TokenType
import xyz.malefic.kanman.data.errorLens
import xyz.malefic.kanman.data.errorModel
import xyz.malefic.kanman.data.transaction.findValidToken
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
                Response(UNAUTHORIZED).with(errorLens of "Missing bearer token".errorModel)
            } else {
                val userId = transaction { findValidToken(token, TokenType.ACCESS)?.user?.id?.value }
                if (userId == null) {
                    Response(UNAUTHORIZED).with(errorLens of "Invalid or expired token".errorModel)
                } else {
                    next(request.with(authenticatedUserId of userId))
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
