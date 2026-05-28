package xyz.malefic.kanman

import java.util.UUID

/**
 * Data class containing a sticky note.
 *
 * @param id Unique identifier for the sticky note.
 * @param title Title representing the sticky note.
 * @param content Content of the sticky note.
 */
data class Sticky(
    val id: Int,
    val title: String,
    val content: String,
)

/**
 * Enum representing the columns in a Kanban board.
 */
enum class Column {
    BACKLOG,
    PLANNING,
    IN_PROGRESS,
    DONE,
}

/**
 * Enum representing the visibility of a Kanban board.
 */
enum class Visibility {
    PUBLIC,
    PRIVATE,
}

typealias Stickies = MutableMap<Column, MutableList<Sticky>>

/**
 * Data class representing a Kanban board.
 *
 * @param uuid Unique identifier for the board.
 * @param title Title of the board.
 * @param visibility Visibility of the board.
 * @param columns Columns containing the Sticky's of the board.
 */
data class Board(
    val uuid: UUID,
    val title: String,
    val visibility: Visibility,
    val columns: Stickies,
)
