<!-- Project documentation for the FinFly Firefly III Android companion. -->
# FinFly

FinFly is an offline-first Android companion for a self-hosted Firefly III personal-finance server. Phase 3 fixes transaction search and drawer navigation, adds server-backed tag editing/filtering, makes Dashboard summaries configurable, and applies consistent loading, empty, and retryable error treatments to the Phase 2 foundation.

The presentation takes visual cues from PennyWise AI—generous rounded cards, soft Rose Pine accents, high-contrast dark surfaces, and compact bottom navigation—without copying its business logic.

## Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│ presentation/                                               │
│ Compose screens → screen UiState → Hilt ViewModels          │
└──────────────────────────────┬──────────────────────────────┘
                               │ invokes
┌──────────────────────────────▼──────────────────────────────┐
│ domain/                                                     │
│ pure models → use cases → repository/gateway interfaces     │
│                    Result<T> boundary                        │
└──────────────────────────────┬──────────────────────────────┘
                               │ implemented by
┌──────────────────────────────▼──────────────────────────────┐
│ data/                                                       │
│ Retrofit API + OkHttp interceptors                          │
│ Room DAOs/cache + DataStore settings + repository impls     │
└───────────────┬──────────────────────────────┬──────────────┘
                │ remote                       │ local
        ┌───────▼────────┐              ┌──────▼──────────┐
        │ Firefly III API│              │ Room / DataStore│
        └────────────────┘              └─────────────────┘
```

Dependencies point inward: presentation knows domain, data implements domain contracts, and domain has no Android dependency. Hilt owns construction. Coroutines and Flow carry asynchronous work. UI code receives `Result<T>`-derived states and never handles raw exceptions.

## Module breakdown

This foundation uses one Android Gradle module with strict source-layer packages, keeping future modularization mechanical:

- `data/local`: Room entities, DAOs, and database.
- `data/network`: the extensible Retrofit interface, DTOs, dynamic server routing, authentication, and connection tester.
- `data/repository`: offline-first repository implementations.
- `data/settings`: DataStore persistence.
- `domain/model`: Android-free finance and sync models.
- `domain/repository`: repository and gateway contracts.
- `domain/usecase`: documented business rules.
- `presentation`: theme, navigation, reusable composables, screen states, screens, and ViewModels.
- `di`: Hilt providers and bindings.

## Build and run

1. Install Android SDK 36 and JDK 17.
2. Open the root directory in Android Studio or run `./gradlew assembleDebug` (`gradlew.bat assembleDebug` on Windows).
3. Install the debug APK from `app/build/outputs/apk/debug/app-debug.apk`.
4. Open Settings, enter the complete Firefly III URL and a personal access token, test the connection, and save.
5. Open Dashboard or pull to refresh. The app caches fetched accounts, categories, and transactions for offline display.

Every push to `main`, pull request, or manual workflow run executes unit tests, Android lint, and debug/release APK builds in GitHub Actions. Successful runs publish `finfly-debug-apk` and unsigned `finfly-release-apk` artifacts for seven days, so no local Android toolchain is required for CI verification.

Cleartext HTTP is enabled for trusted local-network Firefly installations. Prefer HTTPS whenever the server is reachable outside a private LAN.

## Add a Firefly API endpoint

1. Add request/response DTOs in `data/network/dto`; keep Firefly field names there.
2. Add one Retrofit method to `FireflyApiService`. Existing endpoints and interceptors require no edits.
3. Map the DTO to a Room entity or domain model in `data/mapper`.
4. Add the capability to the relevant domain repository interface and implementation.
5. Wrap every return path in domain `Result<T>` and expose asynchronous values through Flow.
6. Put orchestration or validation in a focused, documented use case; consume that use case from a ViewModel.

The server URL and bearer token are applied centrally, so endpoint methods never manage connection configuration.

## Add a bank SMS parser (later phase)

SMS access and parsing remain intentionally absent through Phase 3. A later phase can introduce a separate parser feature with this contract:

```text
BankSmsParser (domain interface)
    ├── HdfcSmsParser
    ├── IciciSmsParser
    └── NewBankSmsParser  ← one new file

Hilt multibinding: @IntoSet BankSmsParser
ParserRegistry: Set<@JvmSuppressWildcards BankSmsParser>
```

Each parser declares whether it supports a sender/message and returns a domain parse result. Bind each implementation into a Hilt set from its own module. `ParserRegistry` receives the set, which means adding a bank requires one parser file plus its colocated binding and no registry edits. SMS permissions, receivers, parser code, and `rawSms` population remain deferred to that later SMS phase.

## Swap an AI provider (Phase 4)

AI is intentionally absent from this foundation. Phase 4 should define an Android-free `FinanceAssistant` interface in domain and inject its selected implementation through Hilt:

```text
UseCase → FinanceAssistant interface
                  ▲
                  ├── OllamaFinanceAssistant
                  ├── OpenAiCompatibleFinanceAssistant
                  └── OnDeviceFinanceAssistant
```

Provider configuration belongs in data/settings; prompts, HTTP clients, and model SDKs stay in provider-specific data packages. Swapping the Hilt binding changes the provider without touching use cases, ViewModels, or screens.

## Phase boundaries

Phase 1 implemented the foundation, theme, domain models, Firefly API, Room cache, repositories, DataStore settings, type-safe bottom navigation, initial Dashboard, and transaction list.

Phase 2 implemented compact category/type transaction filters, scrollable transaction tag pills, full transaction detail and constrained Firefly editing, a persistent drawer and sync app bar, grouped account browsing with account-filtered transactions, budget/category/bill/piggy-bank lists, 90-day synchronization, weekly and category Dashboard charts, asset/liability totals, and debug/release CI artifacts.

Phase 3 fixes debounced transaction search and drawer route state, adds a dedicated tag repository with edit/filter selection, removes Dashboard account cards, makes net-worth and recent-transaction sections configurable, separates reference/raw-SMS detail fields, and standardizes skeleton loading, illustrated empty states, and retryable error cards.

Deferred by design after Phase 3: SMS reading/parsing, AI assistance, the dedicated Reports experience, and notifications.
