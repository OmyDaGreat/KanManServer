package xyz.malefic.kanman.data

import kotlinx.serialization.Serializable
import org.http4k.core.Body
import org.http4k.format.KotlinxSerialization.auto
import kotlin.uuid.Uuid

@Serializable
data class ErrorModel(
    val error: String,
)

val String.error: ErrorModel
    get() = ErrorModel(this)

val errorLens = Body.auto<ErrorModel>().toLens()

@Serializable
data class UserModel(
    val id: Uuid,
    val username: String,
)

fun UserEntity.toModel(): UserModel = UserModel(id.value, username)

val userLens = Body.auto<UserModel>().toLens()

@Serializable
data class LoginModel(
    val username: String,
    val password: String,
)

val loginLens = Body.auto<LoginModel>().toLens()

@Serializable
data class BoardModel(
    val id: Uuid,
    val title: String,
    val visibility: String,
)

fun BoardEntity.toModel() = BoardModel(id.value, title, visibility.name)

val boardLens = Body.auto<BoardModel>().toLens()

@Serializable
data class BoardsListModel(
    val username: String,
    val boards: List<BoardModel>,
)

val boardsListLens = Body.auto<BoardsListModel>().toLens()
