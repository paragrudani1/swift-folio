package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun `access key rules`() {
        assertEquals(FieldError.EMPTY, CredentialValidator.accessKeyId(""))
        assertEquals(FieldError.TOO_SHORT, CredentialValidator.accessKeyId("AKIA123"))
        assertEquals(FieldError.INVALID_CHARACTERS, CredentialValidator.accessKeyId("AKIA 1234567890123"))
        assertEquals(FieldError.TOO_LONG, CredentialValidator.accessKeyId("A".repeat(129)))
        assertNull(CredentialValidator.accessKeyId("AKIAIOSFODNN7EXAMPLE"))
    }

    @Test fun `secret key rules`() {
        assertEquals(FieldError.EMPTY, CredentialValidator.secretAccessKey(" "))
        assertEquals(FieldError.TOO_SHORT, CredentialValidator.secretAccessKey("x".repeat(39)))
        assertEquals(FieldError.INVALID_CHARACTERS, CredentialValidator.secretAccessKey("x".repeat(39) + " y"))
        assertNull(CredentialValidator.secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"))
    }

    @Test fun `bucket name rules`() {
        assertNull(BucketNameValidator.validate("my-bucket.2026"))
        assertEquals(FieldError.EMPTY, BucketNameValidator.validate(""))
        assertEquals(FieldError.TOO_SHORT, BucketNameValidator.validate("ab"))
        assertEquals(FieldError.TOO_LONG, BucketNameValidator.validate("a".repeat(64)))
        assertEquals(FieldError.UPPERCASE, BucketNameValidator.validate("MyBucket"))
        assertEquals(FieldError.INVALID_CHARACTERS, BucketNameValidator.validate("my_bucket"))
        assertEquals(FieldError.BAD_START_OR_END, BucketNameValidator.validate("-bucket"))
        assertEquals(FieldError.BAD_START_OR_END, BucketNameValidator.validate("bucket."))
        assertEquals(FieldError.CONSECUTIVE_DOTS, BucketNameValidator.validate("my..bucket"))
        assertEquals(FieldError.IP_ADDRESS_FORMAT, BucketNameValidator.validate("192.168.5.4"))
        assertEquals(FieldError.RESERVED_NAME, BucketNameValidator.validate("xn--bucket"))
        assertEquals(FieldError.RESERVED_NAME, BucketNameValidator.validate("bucket-s3alias"))
    }

    @Test fun `folder name rules`() {
        val existing = listOf("photos", "Docs")
        assertEquals(FieldError.EMPTY, FolderNameValidator.validate("   ", "", existing))
        assertEquals(FieldError.CONTAINS_SLASH, FolderNameValidator.validate("a/b", "", existing))
        assertEquals(FieldError.DOT_SEGMENT, FolderNameValidator.validate("..", "", existing))
        assertEquals(FieldError.ALREADY_EXISTS, FolderNameValidator.validate("photos", "", existing))
        assertNull(FolderNameValidator.validate("docs", "", existing))
        assertEquals(FieldError.KEY_TOO_LONG, FolderNameValidator.validate("x".repeat(1024), "", existing))
        assertNull(FolderNameValidator.validate(" trimmed ", "p/", existing))
    }

    @Test fun `outer whitespace is a warning`() {
        assertTrue(FolderNameValidator.hasOuterWhitespace(" a"))
        assertFalse(FolderNameValidator.hasOuterWhitespace("a b"))
        assertFalse(FolderNameValidator.hasOuterWhitespace(""))
    }

    @Test fun `connection config validity and redaction`() {
        val config = ConnectionConfig("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "media", "eu-west-1")
        assertTrue(config.isValid)
        assertFalse(config.toString().contains(config.secretAccessKey))
        assertFalse(config.toString().contains(config.accessKeyId))
        assertEquals("••••MPLE", config.maskedAccessKeyId)
        assertFalse(config.copy(bucket = "B").isValid)
    }
}
