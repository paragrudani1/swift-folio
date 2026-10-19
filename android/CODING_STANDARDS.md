# Coding standards (Swift Folio Android)

These rules apply to every change. Reviewers check them before style nits.

## Structure
- **One concern per class or file.** ViewModels hold UI state and delegate to use cases (`bucket/`, `SignOut`),
  `SessionManager` and `TransferScheduler`. They make no S3 calls and no Android framework calls. Composables contain
  layout only: no business rules, no I/O. Rules a screen shows (hold-to-confirm, "all selected", queue totals) are
  derived properties of its `UiState`; pure state changes live in small extension functions (`BrowserStateTransitions`).
- **Pure logic lives in `:core`** (a plain Kotlin/JVM module with no Android dependencies), with JUnit tests. This covers paths, sorting,
  filtering, validation, formatting, error classification and upload planning.
- **Interfaces only at real seams:** `S3Repository` (+ `S3RepositoryFactory`), `CredentialStore` (+ `PayloadCipher`),
  `SettingsStore`, `TransferScheduler`, the transfer runner's `UploadSourceProvider`, `DownloadTargets`, `OutcomeLog`
  and `TransferCredentials`, and `ActiveRepository`. The AWS, Keystore/DataStore, content resolver, MediaStore and
  WorkManager classes are the adapters. Use cases expose a small interface each (`FolderListings`, `StorageTracker`,
  `BucketMutations`, `SignOut`) so ViewModels can be tested with fakes or the real use cases over a fake repository.
  Model types never leak across a seam: `BatchRunner` works with `BatchSnapshot`, and only `TransferProgressCodec`
  knows WorkManager `Data`.
- **Dependency inversion:** `AppContainer` is the hand-written composition root, created once by the
  `Application`. Objects receive dependencies through constructors. ViewModels are built by factories in the nav host;
  workers by `TransferWorkerFactory`, which the Application registers through `Configuration.Provider`. There are no
  static singletons, no `Context` extension or `lateinit` service locators and no DI framework.

## Open/closed
Variation is table- or sealed-type driven, so adding a case never edits branching logic in several places:
- `FileKind` extensions and `FileTypeStyles` hold file types.
- `SortField` comparators and `sortFieldLabels` hold sort options.
- The `S3ErrorMapper` rules and `ErrorTexts` map error kinds.
- `CommandMode` defines the command-bar modes, and `BrowserAction` families give exhaustive `when` blocks with no `else`.

## State and errors
- Each screen has one immutable `UiState` data class with `val` fields, exposed as `StateFlow` and
  updated only with `copy`. User intents are a sealed `Action` type. One-shot navigation is a sealed `Effect`
  sent through a `Channel`.
- Exceptions never cross a seam. Repository operations return `S3Result` (`Success` / `Failure(ConnectError)`),
  credential operations `CredentialResult`; stores log I/O failures by type instead of throwing. Coroutine
  cancellation is always rethrown.
- **One place** classifies SDK failures: `toConnectError()` extracts code, status and region header, and
  `core.S3ErrorMapper` classifies them. `ui/ErrorTexts` is the one table from kinds to user strings.
- Never swallow an exception silently. Handle it, attach it with `addSuppressed`, or log its **type** with `Log.w`.
- Never log secrets, keys, URIs or object contents. `ConnectionConfig.toString()` is redacted on purpose.

## Readability
- Keep functions under about 40 lines. Split them by intent, not by line count.
- Use named arguments for every Boolean parameter at call sites (`remember = true`, `isRefresh = false`).
- No magic numbers: use `CoreConstants`, `AppConstants`, theme tokens (`Spacing`, `ConsoleMotion`) or a
  `private const` next to its single use.
- Write KDoc on every public type. For each interface method, state its invariants and failure modes.
- Naming: a **prefix** is a folder path ending in `/` (or `""` for the root), a **key** is a full object key, and
  an **id** is either one (selection, deletion). Booleans read as predicates (`isActive`, `canGoBack`).

## Compose
- Each screen is a stateless `XContent(state, onAction, …)` plus a thin stateful `XScreen(viewModel, …)` wrapper
  that collects state and effects and owns Android launchers.
- Every stateless screen has `@Preview`s in both dark and light theme (`PreviewSurface`).
- Use only stable Compose / Material 3 APIs from BOM 2024.12.01. `@OptIn(ExperimentalMaterial3Api::class)` is only
  for the top-app-bar scroll behaviour, `ModalBottomSheet`, `PullToRefreshBox` and `ExposedDropdownMenuBox`.
- All UI text goes in `res/values/strings.xml` (with plurals for counts). Every icon-only control has a content description.
  Touch targets are at least 48dp.

## Tests
- Test `:core` with JUnit 4. Test ViewModels, use cases, `SessionManager` and the transfer runner with the fakes in
  `app/src/test/.../fakes`, `kotlinx-coroutines-test` and `MainDispatcherRule`; file-backed stores run against a
  `TemporaryFolder`. Collect `StateFlow.value` directly; Turbine is not used.
- Every behaviour change that can run on the JVM gets a unit test.
