package io.swiftfolio.app.fakes

import io.swiftfolio.core.ConnectionConfig
import io.swiftfolio.core.ObjectEntry

/** Shared fixtures. */
object TestData {
    const val ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE"
    const val SECRET = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
    const val BUCKET = "media-prod"

    val config = ConnectionConfig(ACCESS_KEY, SECRET, BUCKET, "us-east-1")

    val photos = ObjectEntry.Folder("photos/")
    val docs = ObjectEntry.Folder("docs/")
    val readme = ObjectEntry.File("README.md", sizeBytes = 120, lastModifiedEpochMillis = 1_000)
    val root: List<ObjectEntry> = listOf(photos, docs, readme)

    val sunset = ObjectEntry.File("photos/sunset.jpg", sizeBytes = 4_000, lastModifiedEpochMillis = 2_000)
    val photosListing: List<ObjectEntry> = listOf(ObjectEntry.Folder("photos/2026/"), sunset)
}
