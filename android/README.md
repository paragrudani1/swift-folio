# Swift Folio for Android

A native Android client for **Swift Folio**, the AWS S3 bucket manager in this repository. It is
built with Kotlin and Jetpack Compose (Material 3), runs on phones (minSdk 26, targetSdk 35), and has dark and light themes.
See [DESIGN.md](DESIGN.md) for the visual system and [CODING_STANDARDS.md](CODING_STANDARDS.md)
for contribution rules.

## Features

- **Connect.** The form takes an access key ID (16+ characters), a secret (40+, with a reveal toggle that re-masks after 10s and
  on backgrounding), a bucket (S3 naming rules) and a region (dropdown of the standard regions). It validates inline after
  each field loses focus. A live checklist (resolving region → signing request → listing bucket) shows connection progress.
  Errors are categorised: invalid key, signature mismatch, expired, clock skew, no such bucket, access denied, network
  or timeout, and wrong region. A wrong-region error comes with a one-tap **Switch to \<region\>** fix.
- **Remember credentials.** Off until you turn it on. Credentials are encrypted with an Android Keystore AES-GCM key.
  When they are saved, the app auto-connects on launch. If the Keystore or the file refuses the save, you stay connected
  and "Remember" switches itself off.
- **Browser.**
  - The folder name is the headline of a collapsing top bar. A floating path pill lets you tap a segment to jump there,
    and long-press to copy the `s3://` URI.
  - System back goes up one folder and then exits.
  - Folders come first, with file-type tiles, size and modified date. Sort by name, modified or size (asc/desc).
  - Search filters the current folder, with Files only / Folders only filters.
  - Pull-to-refresh, a bucket storage total, and skeleton, empty, error and stale states. The total is computed by
    listing the bucket once on connect (tap the chip to recompute); deletes, new folders and finished uploads adjust
    it locally instead of rescanning. Overwriting an existing key counts the upload again until the next recompute.
  - The open folder survives process death.
- **Multi-select.** Long-press to enter, tap to toggle, select all. Bulk download, copy keys, and bulk delete
  with a confirmation. Folders delete recursively in batches of 1000, using hold-to-confirm. There is no swipe-to-delete.
- **Uploads.** Pick one or many files. Files up to 5 MiB use one PutObject; larger files use a multipart upload with
  5 MiB parts and CRC32 checksums, which is aborted on cancel or failure. Progress is shown per file and overall, and
  you can cancel per file or all. Uploads run in a WorkManager foreground worker, so they survive rotation,
  backgrounding and process death (for remembered sessions). Each finished file is recorded on disk, so a batch that
  WorkManager restarts after process death continues with the files that had not finished; nothing is sent twice.
- **Downloads.** Files are saved to `Download/SwiftFolio`: through MediaStore on API 29+, or on API 26–28 as a file in
  the public Downloads folder when storage permission is granted (asked once, before the first download). Without it,
  API 26–28 downloads go to the app's own storage and the notification says so, because those files are removed on
  uninstall. Progress shows in notifications (notification permission is asked before the first upload or download)
  and in the transfer sheet next to uploads; tapping the completion notification of a single file opens it.
- **Transfer sheet.** Uploads and downloads in one queue with overall and per-file progress, per-file cancel,
  "Cancel all" and "Clear finished" (which only removes batches whose work has ended).
- **Folders.** Create a folder with validation: no slash, not empty, unique, and the full key within 1024 bytes.
- **Settings.** Theme (System / Light / Dark), remember on/off (off wipes the saved keys), sign out with an optional
  "forget credentials" (signing out cancels running transfers first), and the app version.

## Build

Requirements: **Android Studio Ladybug (2024.2) or newer** and **JDK 17**.

```bash
cd android
./gradlew :app:assembleDebug          # APK in app/build/outputs/apk/debug/
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug
```

To install on a device or emulator, run `./gradlew :app:installDebug`.

### CI and downloading the APK

`.github/workflows/android.yml` runs on every push or pull request that touches `android/**`. It runs:

```
./gradlew --no-daemon :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

To get the APK, open the workflow run in **GitHub → Actions → Android** and download the
**`swift-folio-debug-apk`** artifact (a zip containing `app-debug.apk`). Lint and test reports are in
the `swift-folio-reports` artifact.

## Security notes

- Keys never leave the device. They go only to AWS over HTTPS (the AWS SDK for Kotlin with SigV4).
- With "Remember" on, the config is serialised to JSON and encrypted with **AES-256-GCM**. The key is
  generated inside the **Android Keystore** and is non-exportable. Only the ciphertext and IV (Base64) are stored in
  DataStore. If the Keystore key becomes invalid, the saved entry is discarded rather than crashing the app.
- Turning "Remember" off, or signing out with "forget", deletes the stored ciphertext immediately.
- App backup and device transfer are disabled, because a Keystore-bound ciphertext is useless elsewhere.
- Secrets are never logged. `ConnectionConfig.toString()`, `ConnectUiState.toString()` and the secret-edit action are
  redacted, and the revealed secret is masked again as soon as the screen is paused. WorkManager input data holds only a
  batch id. Background workers get credentials from memory or the encrypted store and build their own S3 client.
- Corrupt preference files are replaced with empty ones instead of crashing (`ReplaceFileCorruptionHandler`).
- The app needs the IAM permissions `s3:ListBucket`, `s3:GetObject`, `s3:PutObject` and `s3:DeleteObject` on the bucket.

### Incomplete multipart uploads

A multipart upload is aborted when it fails or is cancelled. If the process is killed in the middle of one, the parts
already sent stay in the bucket (and are billed) until they are aborted, and the app does not list or clean them up
(that would need `s3:ListBucketMultipartUploads` and `s3:AbortMultipartUpload`). Add a lifecycle rule to the bucket that
aborts incomplete multipart uploads, for example after one day:

```json
{
  "Rules": [
    {
      "ID": "abort-incomplete-multipart-uploads",
      "Status": "Enabled",
      "Filter": {},
      "AbortIncompleteMultipartUpload": { "DaysAfterInitiation": 1 }
    }
  ]
}
```

```bash
aws s3api put-bucket-lifecycle-configuration --bucket <bucket> --lifecycle-configuration file://lifecycle.json
```

This replaces any existing lifecycle configuration of the bucket, so merge the rule into it if there is one.

## Architecture

```
android/
├── core/   Pure Kotlin/JVM (no Android): S3Path, ObjectEntry, Listing (sort/filter), FileKind,
│           ByteFormat/TimeFormat, validators, S3ErrorMapper → ConnectError, S3Result,
│           UploadPlanner, Breadcrumbs, AwsRegions. 50 JUnit tests.
└── app/    Single-activity Compose app
    ├── AppContainer            hand-written composition root (created by SwiftFolioApplication)
    ├── data/                   seams: S3Repository, CredentialStore, SettingsStore
    │   ├── aws/                AwsS3Repository + AwsUploader (S3Client adapter), error extraction
    │   ├── security/           KeystoreCipher (AES/GCM) + KeystoreCredentialStore (DataStore)
    │   └── settings/           DataStoreSettingsStore
    ├── session/                SessionManager (connect / restore / remember; owns the screens' repository),
    │                           SignOut use case (cancels transfers first), ActiveRepository seam
    ├── bucket/                 browser use cases: FolderListings (per-prefix cache), StorageTracker
    │                           (scan once, adjust locally), BucketMutations (create folder, delete, uploads)
    ├── transfer/               TransferScheduler seam, WorkManager adapter, TransferWorker + TransferWorkerFactory
    │                           (foreground, dataSync), BatchRunner, manifests + per-file outcome log,
    │                           totals-only progress codec, download targets, notifications
    └── ui/                     theme (Console tokens), components, connect/, browser/ (+ sheets),
                                settings/, navigation/ (NavHost, AppViewModel)
```

- **Flow of a request:** `Composable → ViewModel.onAction → use case (bucket/, SignOut) / SessionManager /
  TransferScheduler → S3Repository → S3Result → immutable UiState → Composable`. ViewModels make no S3 calls.
  One-shot navigation goes through a sealed `Effect` channel.
- **Navigation:** Connect → Browser (Connect is popped) → Settings. Folder navigation is state inside the browser
  (a prefix kept in `SavedStateHandle`, with a per-prefix listing cache), not navigation destinations.
- **Background work:** the Application implements `Configuration.Provider` and hands WorkManager the container's
  `TransferWorkerFactory`, so workers get their dependencies through constructors. The default WorkManager startup
  initializer is removed in the manifest. Per-file state lives in the transfer directory (one manifest and one
  append-only outcome log per batch); WorkManager progress and output data carry five fixed-size totals, far below
  its 10 KB limit whatever the batch size.
- **Tests:** `:core` (JUnit 4). `:app` unit tests cover `SessionManager`, sign-out, the credential store (with a fake
  DataStore and cipher), the browser use cases, the transfer runner, manifest store, progress codec and clean-up
  selection, and the Connect, Browser and Settings ViewModels, using fakes of every seam (`app/src/test/.../fakes`)
  and `kotlinx-coroutines-test`.

## Dependencies

AGP 8.7.3 · Kotlin 2.1.0 (Compose and serialization plugins) · Compose BOM 2024.12.01 · Activity 1.9.3 · Core 1.15.0
· Lifecycle 2.8.7 (incl. viewmodel-savedstate) · Navigation 2.8.5 · DataStore 1.1.1 · WorkManager 2.10.0 · SplashScreen 1.0.1 · Coroutines 1.9.0
· kotlinx-serialization 1.7.3 · desugar_jdk_libs 2.1.3 · **AWS SDK for Kotlin `aws.sdk.kotlin:s3` 1.4.125**.

The AWS SDK is pinned to 1.4.125 on purpose. Releases 1.5 and later are compiled with newer Kotlin versions (1.9.x depends on
`kotlin-stdlib` 2.4.0), and their metadata cannot be read by the Kotlin 2.1.0 compiler. 1.4.125 is the newest release
built with Kotlin 2.1.0. Upgrade it together with Kotlin.

Fonts: Inter and JetBrains Mono, both under the SIL Open Font License 1.1. The license texts are in `app/src/main/assets/fonts/`.

## Verification status

- `:core` was built and its **50 tests pass** locally.
- The `:app` module **could not be built locally**: this environment cannot reach Google Maven, so AGP and AndroidX
  are unavailable. To reduce risk:
  - Every `:app` Kotlin source and the **79 app unit tests** were compiled and run in a scratch JVM harness. It used
    JetBrains Compose Desktop 1.7.3 (the same Compose 1.7 / Material 3 1.3 APIs), the real AWS SDK `s3-jvm` 1.4.125,
    and `android-all` (API 35) from Maven Central.
  - Stubs stood in only for WorkManager, DataStore, Activity, Navigation, NotificationCompat, FileProvider, SplashScreen,
    `ViewModel`, `SavedStateHandle`, `stringResource` and `R`.
  - Resource processing (aapt), the manifest merge and lint ran **only in CI**. The first CI run is the real check.
