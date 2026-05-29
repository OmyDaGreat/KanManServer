package xyz.malefic.kanman

import co.touchlab.kermit.Logger
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs

object SQLKermit : SqlLogger {
    override fun log(
        context: StatementContext,
        transaction: Transaction,
    ) {
        Logger.i(tag = "SQL") { context.expandArgs(transaction) }
    }
}
