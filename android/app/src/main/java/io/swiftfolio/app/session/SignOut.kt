package io.swiftfolio.app.session

import io.swiftfolio.app.transfer.TransferKind
import io.swiftfolio.app.transfer.TransferScheduler

/** Ends the session from any screen. */
fun interface SignOut {
    /**
     * Cancels every queued and running transfer, closes the session and, when
     * [forgetCredentials] is true, deletes the saved credentials. Never throws.
     */
    suspend operator fun invoke(forgetCredentials: Boolean)
}

/**
 * [SignOut] that stops background transfers before the session goes away, so no worker keeps
 * using credentials the user just signed out of.
 */
class CancelTransfersAndSignOut(
    private val sessionManager: SessionManager,
    private val transferScheduler: TransferScheduler,
) : SignOut {
    override suspend fun invoke(forgetCredentials: Boolean) {
        TransferKind.entries.forEach { kind -> transferScheduler.cancelAll(kind) }
        sessionManager.signOut(forgetCredentials)
    }
}
