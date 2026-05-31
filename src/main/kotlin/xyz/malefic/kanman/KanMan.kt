package xyz.malefic.kanman

import org.http4k.routing.poly
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import xyz.malefic.kanman.data.BoardUsers
import xyz.malefic.kanman.data.Boards
import xyz.malefic.kanman.data.SQLKermit
import xyz.malefic.kanman.data.StickyNotes
import xyz.malefic.kanman.data.Users
import xyz.malefic.kanman.http.http

fun main() {
    Database.connect(
        url = "jdbc:sqlite:data.db", // TODO: Actually decide file name
        driver = "org.sqlite.JDBC",
    )

    transaction {
        addLogger(SQLKermit)
        exec("PRAGMA foreign_keys = ON;")
        SchemaUtils.create(Users, Boards, StickyNotes, BoardUsers)
    }

    val server = poly(http, ws).asServer(Undertow(6320)).start()

    println("Server started on port ${server.port()}!")
}
