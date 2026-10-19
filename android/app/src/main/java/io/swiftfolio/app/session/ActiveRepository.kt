package io.swiftfolio.app.session

import io.swiftfolio.app.data.S3Repository

/**
 * Gives use cases the repository of the active session without exposing session lifecycle.
 * Implemented by [SessionManager]; tests pass a lambda.
 */
fun interface ActiveRepository {
    /** The active session's repository, or null when signed out. Callers must not close it. */
    fun current(): S3Repository?
}
