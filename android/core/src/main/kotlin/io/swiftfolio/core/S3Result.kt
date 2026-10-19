package io.swiftfolio.core

/**
 * Outcome of one S3 operation. Repositories return this instead of throwing, so no
 * exception crosses the repository seam (coroutine cancellation is still propagated).
 */
sealed interface S3Result<out T> {
    /** The operation completed and produced [value]. */
    data class Success<out T>(val value: T) : S3Result<T>

    /** The operation failed with a classified [error]. */
    data class Failure(val error: ConnectError) : S3Result<Nothing>
}

/** Transforms the success value, keeping failures as they are. */
inline fun <T, R> S3Result<T>.map(transform: (T) -> R): S3Result<R> = when (this) {
    is S3Result.Success -> S3Result.Success(transform(value))
    is S3Result.Failure -> this
}

/** Runs [action] on success and returns the receiver unchanged. */
inline fun <T> S3Result<T>.onSuccess(action: (T) -> Unit): S3Result<T> {
    if (this is S3Result.Success) action(value)
    return this
}

/** Runs [action] on failure and returns the receiver unchanged. */
inline fun <T> S3Result<T>.onFailure(action: (ConnectError) -> Unit): S3Result<T> {
    if (this is S3Result.Failure) action(error)
    return this
}

/** The success value or null. */
fun <T> S3Result<T>.valueOrNull(): T? = (this as? S3Result.Success)?.value
