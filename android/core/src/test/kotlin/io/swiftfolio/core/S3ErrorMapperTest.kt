package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class S3ErrorMapperTest {
    private fun kindOf(signal: ErrorSignal, region: String? = "us-east-1") = S3ErrorMapper.map(signal, region).kind

    @Test fun `credential codes`() {
        assertEquals(ConnectErrorKind.INVALID_ACCESS_KEY, kindOf(ErrorSignal(errorCode = "InvalidAccessKeyId", httpStatus = 403)))
        assertEquals(ConnectErrorKind.SIGNATURE_MISMATCH, kindOf(ErrorSignal(errorCode = "SignatureDoesNotMatch", httpStatus = 403)))
        assertEquals(ConnectErrorKind.EXPIRED_CREDENTIALS, kindOf(ErrorSignal(errorCode = "ExpiredToken")))
        assertEquals(ConnectErrorKind.CLOCK_SKEW, kindOf(ErrorSignal(errorCode = "RequestTimeTooSkewed")))
        assertTrue(ConnectErrorKind.SIGNATURE_MISMATCH.isCredentialProblem)
    }

    @Test fun `bucket and permission codes`() {
        assertEquals(ConnectErrorKind.NO_SUCH_BUCKET, kindOf(ErrorSignal(errorCode = "NoSuchBucket", httpStatus = 404)))
        assertEquals(ConnectErrorKind.NO_SUCH_BUCKET, kindOf(ErrorSignal(exceptionChain = listOf("NoSuchBucket"))))
        assertEquals(ConnectErrorKind.NOT_FOUND, kindOf(ErrorSignal(httpStatus = 404)))
        assertEquals(ConnectErrorKind.ACCESS_DENIED, kindOf(ErrorSignal(errorCode = "AccessDenied", httpStatus = 403)))
        assertEquals(ConnectErrorKind.ACCESS_DENIED, kindOf(ErrorSignal(httpStatus = 403)))
    }

    @Test fun `permanent redirect suggests the header region`() {
        val error = S3ErrorMapper.map(
            ErrorSignal(errorCode = "PermanentRedirect", httpStatus = 301, bucketRegionHeader = "eu-west-1"),
            configuredRegion = "us-east-1",
        )
        assertEquals(ConnectErrorKind.WRONG_REGION, error.kind)
        assertEquals("eu-west-1", error.suggestedRegion)
        assertEquals("PermanentRedirect", error.code)
    }

    @Test fun `bare 301 from HeadBucket is a wrong region`() {
        val error = S3ErrorMapper.map(ErrorSignal(httpStatus = 301, bucketRegionHeader = "ap-south-1"), "us-east-1")
        assertEquals(ConnectErrorKind.WRONG_REGION, error.kind)
        assertEquals("ap-south-1", error.suggestedRegion)
        assertEquals("HTTP 301", error.code)
    }

    @Test fun `authorization header malformed reads the region from the message`() {
        val message = "The authorization header is malformed; the region 'us-east-1' is wrong; expecting 'eu-central-1'"
        val error = S3ErrorMapper.map(ErrorSignal(errorCode = "AuthorizationHeaderMalformed", httpStatus = 400, message = message), "us-east-1")
        assertEquals(ConnectErrorKind.WRONG_REGION, error.kind)
        assertEquals("eu-central-1", error.suggestedRegion)
    }

    @Test fun `matching region header on 403 is not a wrong region`() {
        val error = S3ErrorMapper.map(ErrorSignal(httpStatus = 403, bucketRegionHeader = "us-east-1"), "us-east-1")
        assertEquals(ConnectErrorKind.ACCESS_DENIED, error.kind)
        assertNull(error.suggestedRegion)
    }

    @Test fun `network and timeout from the cause chain`() {
        val offline = ErrorSignal.fromThrowable(RuntimeException("wrapper", UnknownHostException("s3.amazonaws.com")))
        val error = S3ErrorMapper.map(offline)
        assertEquals(ConnectErrorKind.NETWORK, error.kind)
        assertEquals("UnknownHostException", error.code)
        assertTrue(error.kind.isTransient)
        val slow = ErrorSignal.fromThrowable(IllegalStateException(SocketTimeoutException("timeout")))
        assertEquals(ConnectErrorKind.TIMEOUT, S3ErrorMapper.map(slow).kind)
    }

    @Test fun `server errors and unknown fallback`() {
        assertEquals(ConnectErrorKind.SERVICE_UNAVAILABLE, kindOf(ErrorSignal(httpStatus = 503)))
        assertEquals(ConnectErrorKind.SERVICE_UNAVAILABLE, kindOf(ErrorSignal(errorCode = "SlowDown")))
        val unknown = S3ErrorMapper.map(ErrorSignal.fromThrowable(IllegalArgumentException("boom")))
        assertEquals(ConnectErrorKind.UNKNOWN, unknown.kind)
        assertEquals("IllegalArgumentException", unknown.code)
        assertEquals("boom", unknown.detail)
    }

    @Test fun `fromThrowable stops on self-referencing causes`() {
        val signal = ErrorSignal.fromThrowable(SelfCause())
        assertEquals(listOf("SelfCause"), signal.exceptionChain)
    }

    private class SelfCause : Exception("loop") {
        override val cause: Throwable get() = this
    }
}
