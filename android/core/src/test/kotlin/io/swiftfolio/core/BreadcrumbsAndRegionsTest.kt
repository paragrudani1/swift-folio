package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreadcrumbsAndRegionsTest {
    @Test fun `short paths show every crumb`() {
        val crumbs = Breadcrumbs.build("media", "photos/2026/")
        assertEquals(listOf("media", "photos", "2026"), crumbs.map { it.label })
        assertEquals(listOf("", "photos/", "photos/2026/"), crumbs.map { it.prefix })
        assertEquals(listOf(false, false, true), crumbs.map { it.isCurrent })
    }

    @Test fun `deep paths collapse the middle into one ellipsis`() {
        val crumbs = Breadcrumbs.build("media", "a/b/c/d/")
        assertEquals(listOf("media", "…", "c", "d"), crumbs.map { it.label })
        val ellipsis = crumbs[1]
        assertTrue(ellipsis.isEllipsis)
        assertEquals("a/b/", ellipsis.prefix)
    }

    @Test fun `root is a single current crumb`() {
        val crumbs = Breadcrumbs.build("media", "")
        assertEquals(1, crumbs.size)
        assertTrue(crumbs.single().isCurrent)
    }

    @Test fun `regions default and search`() {
        assertEquals(CoreConstants.DEFAULT_REGION, AwsRegions.all.first().code)
        assertEquals("Ireland", AwsRegions.find("eu-west-1")?.location)
        assertNull(AwsRegions.find("mars-1"))
        assertTrue(AwsRegions.search("tokyo").any { it.code == "ap-northeast-1" })
        assertEquals(AwsRegions.all, AwsRegions.search(""))
        assertEquals(AwsRegions.all.size, AwsRegions.all.map { it.code }.toSet().size)
    }
}
