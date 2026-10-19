package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ListingTest {
    private val folderB = ObjectEntry.Folder("p/beta/")
    private val folderA = ObjectEntry.Folder("p/Alpha/")
    private val file10 = ObjectEntry.File("p/file10.txt", sizeBytes = 10, lastModifiedEpochMillis = 300)
    private val file2 = ObjectEntry.File("p/file2.txt", sizeBytes = 2_000, lastModifiedEpochMillis = 100)
    private val fileNoDate = ObjectEntry.File("p/zeta.bin", sizeBytes = 500, lastModifiedEpochMillis = null)
    private val entries = listOf(file10, folderB, fileNoDate, folderA, file2)

    @Test fun `fromRaw keeps direct children and drops markers and self`() {
        val rows = Listing.fromRaw(
            prefix = "p/",
            commonPrefixes = listOf("p/", "p/a/", "p/a/", "p/ /", "q/x/"),
            objects = listOf(RawObject("p/", 0, null), RawObject("p/x.txt", 3, 1), RawObject("p/dir/", 0, null)),
        )
        assertEquals(listOf(ObjectEntry.Folder("p/a/"), ObjectEntry.File("p/x.txt", 3, 1)), rows)
    }

    @Test fun `fromRaw clamps negative sizes`() {
        val rows = Listing.fromRaw("", emptyList(), listOf(RawObject("a", -5, null)))
        assertEquals(ObjectEntry.File("a", 0, null), rows.single())
    }

    @Test fun `name sort is natural, case-insensitive and folders first`() {
        val sorted = Listing.sort(entries, SortOrder(SortField.NAME, SortDirection.ASCENDING))
        assertEquals(listOf(folderA, folderB, file2, file10, fileNoDate), sorted)
    }

    @Test fun `descending name sort reverses folders and files separately`() {
        val sorted = Listing.sort(entries, SortOrder(SortField.NAME, SortDirection.DESCENDING))
        assertEquals(listOf(folderB, folderA, fileNoDate, file10, file2), sorted)
    }

    @Test fun `size and modified sorts only reorder files`() {
        assertEquals(
            listOf(folderA, folderB, file10, fileNoDate, file2),
            Listing.sort(entries, SortOrder(SortField.SIZE)),
        )
        assertEquals(
            listOf(folderA, folderB, file10, file2, fileNoDate),
            Listing.sort(entries, SortOrder(SortField.MODIFIED, SortDirection.DESCENDING)),
        )
    }

    @Test fun `select flips the active field and resets others to ascending`() {
        val byName = SortOrder()
        assertEquals(SortOrder(SortField.NAME, SortDirection.DESCENDING), byName.select(SortField.NAME))
        assertEquals(SortOrder(SortField.SIZE, SortDirection.ASCENDING), byName.select(SortField.SIZE))
    }

    @Test fun `filter is case-insensitive and respects type filter`() {
        assertEquals(listOf(file10, file2), Listing.filter(entries, "FILE"))
        assertEquals(listOf(folderB, folderA), Listing.filter(entries, "", EntryTypeFilter.FOLDERS_ONLY))
        assertEquals(listOf(file10, fileNoDate, file2), Listing.filter(entries, "  ", EntryTypeFilter.FILES_ONLY))
    }

    @Test fun `visible filters then sorts`() {
        val rows = Listing.visible(entries, "a", SortOrder(), EntryTypeFilter.ALL)
        assertEquals(listOf(folderA, folderB, fileNoDate), rows)
    }

    @Test fun `matchRanges finds every case-insensitive occurrence`() {
        assertEquals(listOf(0..1, 4..5), Listing.matchRanges("abxxAB", "ab"))
        assertEquals(emptyList<IntRange>(), Listing.matchRanges("abc", " "))
    }
}
