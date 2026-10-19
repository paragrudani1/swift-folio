package io.swiftfolio.app.bucket

import io.swiftfolio.app.session.ActiveRepository

/**
 * The use cases one browser screen works with. They share one listing cache and one storage
 * total, so a new set is created for each screen instance.
 */
data class BrowserUseCases(
    val listings: FolderListings,
    val storage: StorageTracker,
    val mutations: BucketMutations,
) {
    companion object {
        /** Builds a connected set over the repository of [activeRepository]. */
        fun create(activeRepository: ActiveRepository): BrowserUseCases {
            val listings = CachedFolderListings(activeRepository)
            val storage = BucketStorageTracker(activeRepository)
            return BrowserUseCases(listings, storage, RepositoryBucketMutations(activeRepository, listings, storage))
        }
    }
}
