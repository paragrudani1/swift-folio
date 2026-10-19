package io.swiftfolio.app.data.aws

import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ErrorSignal
import io.swiftfolio.core.S3ErrorMapper

/** Header S3 uses to say which region a bucket lives in. */
private const val BUCKET_REGION_HEADER = "x-amz-bucket-region"

/**
 * Extracts the AWS-specific fields (error code, HTTP status, region header) from an SDK
 * exception chain and classifies it with the shared [S3ErrorMapper]. This is the only
 * place that knows SDK exception types.
 */
internal fun Throwable.toConnectError(configuredRegion: String?): ConnectError {
    val service = generateSequence(this) { it.cause.takeIf { cause -> cause !== it } }
        .filterIsInstance<ServiceException>()
        .firstOrNull()
    val metadata = service?.sdkErrorMetadata
    val response = metadata?.protocolResponse as? HttpResponse
    val signal = ErrorSignal.fromThrowable(
        throwable = this,
        errorCode = metadata?.errorCode,
        httpStatus = response?.status?.value,
        bucketRegionHeader = response?.headers?.get(BUCKET_REGION_HEADER),
    )
    return S3ErrorMapper.map(signal, configuredRegion)
}
