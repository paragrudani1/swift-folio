package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class S3PathTest {
    @Test fun `normalizePrefix collapses slashes and adds trailing slash`() {
        assertEquals("", S3Path.normalizePrefix(""))
        assertEquals("", S3Path.normalizePrefix("/"))
        assertEquals("a/b/", S3Path.normalizePrefix("/a//b"))
        assertEquals("a/", S3Path.normalizePrefix("a/"))
    }

    @Test fun `parentOf walks up one level and root is its own parent`() {
        assertEquals("", S3Path.parentOf(""))
        assertEquals("", S3Path.parentOf("photos/"))
        assertEquals("photos/", S3Path.parentOf("photos/2026/"))
        assertEquals("a/b/", S3Path.parentOf("a/b/c/"))
    }

    @Test fun `segments ancestors and depth`() {
        assertEquals(listOf("a", "b"), S3Path.segments("a/b/"))
        assertEquals(listOf("", "a/", "a/b/"), S3Path.ancestors("a/b/"))
        assertEquals(listOf(""), S3Path.ancestors(""))
        assertEquals(0, S3Path.depth(""))
        assertEquals(2, S3Path.depth("a/b/"))
    }

    @Test fun `join and childPrefix`() {
        assertEquals("a/file.txt", S3Path.join("a/", "file.txt"))
        assertEquals("file.txt", S3Path.join("", "/file.txt"))
        assertEquals("a/new/", S3Path.childPrefix("a/", "new"))
        assertEquals("new/", S3Path.childPrefix("", "new/"))
    }

    @Test fun `names and extensions`() {
        assertEquals("b", S3Path.folderName("a/b/"))
        assertEquals("", S3Path.folderName(""))
        assertEquals("c.TXT", S3Path.fileName("a/b/c.TXT"))
        assertEquals("txt", S3Path.extension("c.TXT"))
        assertEquals("gz", S3Path.extension("backup.tar.gz"))
        assertNull(S3Path.extension(".bashrc"))
        assertNull(S3Path.extension("README"))
        assertNull(S3Path.extension("trailing."))
    }

    @Test fun `s3Uri and folder markers`() {
        assertEquals("s3://media/photos/a.jpg", S3Path.s3Uri("media", "photos/a.jpg"))
        assertEquals("s3://media/", S3Path.s3Uri("media", ""))
        assertTrue(S3Path.isFolderMarker("a/"))
        assertFalse(S3Path.isFolderMarker("a"))
    }
}
