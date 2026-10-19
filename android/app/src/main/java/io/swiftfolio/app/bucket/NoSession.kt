package io.swiftfolio.app.bucket

import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.S3Result

/** Failure returned by the bucket use cases when the session ended while the screen was open. */
internal val NoSessionFailure: S3Result.Failure = S3Result.Failure(ConnectError(ConnectErrorKind.UNKNOWN, "NoSession"))
