# FinFly

FinFly is an offline-first Android companion for a self-hosted Firefly III server. Phase 6 hardens Firefly editing, adds category-and-tag parsing, rich reviewed imports, duplicate protection, and native top-level drawer navigation while keeping AI disabled.

## Architecture

FinFly uses Clean Architecture inside one Android module:

- `presentation`: Jetpack Compose screens, screen state, Hilt ViewModels, and type-safe navigation.
- `domain/model`: Android-free finance, rule, parsing, and sync models.
- `domain/usecase`: business rules, including the independently testable `SmsParserEngine`.
- `domain/repository`: documented persistence, transfer, and network contracts.
- `data/local`: Room entities and DAOs for cached finance data, JSON rule configs, and SMS logs.
- `data/network`: Retrofit, OkHttp, Firefly DTOs, and dynamic server authentication.
- `data/repository`: offline-first repository implementations and Android storage adapters.
- `data/sms`: the friendly-pattern compiler, default rule configs, and the SMS receiver adapter.
- `data/settings`: DataStore-backed connection and display preferences.
- `di`: Hilt providers and bindings.

Dependencies point inward. Domain has no Android dependency, presentation consumes domain contracts, and data implements those contracts. Exceptions are converted to `Result<T>` at repository boundaries.

## Build and run

1. Open the project with Android Studio using JDK 17 and Android SDK 36.
2. Build with `./gradlew assembleDebug`, or push to GitHub and download the CI artifact.
3. Open Settings, enter the Firefly III URL and personal access token, test, and save.
4. Sync once so account and category choices are cached.

Every push to `main`, pull request, or manual workflow run executes unit tests, lint, and debug/release APK builds. Successful runs upload `finfly-debug-apk`, `finfly-release-apk`, and verification reports. No local Android toolchain is required for CI verification.

Cleartext HTTP is enabled for trusted local-network Firefly installations. Prefer HTTPS outside a private LAN.

## Parsing

Automatic processing is off by default. Open **Parsing** from the drawer, grant `RECEIVE_SMS`, map each bank rule to its exact cached Firefly account, and enable the master toggle. The receiver exits immediately while the toggle is off.

For an enabled message:

1. Sender IDs select exact bank-rule matches before partial matches.
2. Friendly debit and credit keywords determine transaction type.
3. `{amount}`, `{description}`, and `{ref}` placeholders extract transaction fields.
4. Enabled category rules are checked by ascending priority.
5. The existing `TransactionRepository` creates the Firefly withdrawal or deposit.
6. Success, skipped, and unmatched outcomes enter the capped 100-row SMS log.

Message text is processed locally. AI and on-device models are not part of Phase 4.

## Add a bank rule

No code or raw regular expression is needed:

1. Open **Parsing** and tap **+** beside Bank Rules.
2. Enter a name and select the exact Firefly account.
3. Add sender IDs and debit/credit keywords using the chip fields.
4. Add friendly patterns such as `Rs.{amount}`, `To {description} On`, and `Ref {ref}`.
5. Paste a real message into **Test this rule** and verify the parsed result.
6. Save and enable the rule.

Text outside placeholders is treated literally. Built-in starting rules cover HDFC Savings, HDFC Credit Card, ICICI Savings, and Edge CSB/Jupiter. New banks require only a new rule.

## Category rules

Category rules match merchant descriptions case-insensitively. Lower priority numbers run first. The Firefly category name must match the server exactly. Default rules cover food, groceries, transport, health, shopping, bills, finance, and gifts.

## Export and import

**Export Rules** writes schema-versioned, human-readable JSON to `Downloads/FinFly/rules_export_<timestamp>.json` on Android 10 and newer. **Import Rules** opens Android's JSON picker, validates schema version 1 and the required bank-rules array, then previews counts.

- **Merge** adds imported rules and skips duplicates by case-insensitive rule name.
- **Replace all** clears existing bank and category rules before importing.

The transfer format contains `version`, `exportedAt`, `bankRules`, and `categoryRules`, leaving a stable boundary for future AI-assisted rule suggestions.

## Add a Firefly endpoint

1. Add request/response DTOs under `data/network/dto`.
2. Add the Retrofit method to `FireflyApiService`.
3. Map remote data to a domain model or Room entity.
4. Extend the appropriate documented domain repository contract.
5. Implement it in data and wrap outcomes in `Result<T>`.
6. Put orchestration in a focused use case and consume it from a ViewModel.

Server URL and bearer-token handling remain centralized in interceptors.

## Phase boundaries

- Phase 1 established the theme, Clean Architecture layers, Firefly API, Room cache, DataStore settings, navigation, Dashboard, and transaction list.
- Phase 2 added filters, transaction detail/editing, drawer navigation, account and organizational-resource browsing, synchronization, charts, and CI artifacts.
- Phase 3 fixed search and drawer state, added tags, configurable Dashboard periods and visuals, creation flows, compact transaction controls, and consistent loading/empty/error states.
- Phase 4 implements JSON-backed BankRule and CategoryRule editing, default Indian-bank rules, placeholder compilation, permission-aware SMS reception, Firefly submission, the capped Room log, JSON merge/replace transfer, and device-versus-UTC display settings.
- Phase 5 adds edit flows for transactions, accounts, budgets, categories, tags, bills, and piggy banks; Firefly rule browsing/editing; transaction budgets; budget limit-versus-spend cards; independent category-chart periods; calendar date selection; one-month report defaults; and confirmed on-demand SMS previews.
- Phase 6 validates ISO currencies across editors, uses Firefly's auto-budget currency on edit, corrects piggy-bank update payloads, surfaces server validation details, adds keyword/global tag rules, carries tags into parsed transactions, shows complete preview metadata and per-row push results, detects likely duplicates, and treats drawer destinations as replaceable top-level screens.
- Phase 6.1 fixes parameterized management-destination restoration, adds multi-category and multi-tag report filters with cash-flow/category transaction drill-downs, and separates parser tags into bank-rule, category-rule, and universal tag scopes.
- Reports provide date-range, category, and tag filters with filtered income, spending, net-flow, monthly cash-flow, and top-category summaries from the offline transaction cache.
- Firefly management includes confirmed deletion for transactions, accounts, budgets, categories, tags, bills, and piggy banks, plus local credential logout.

Deferred after Phase 6: notification-listener inputs, AI rule suggestions, on-device models, and advanced report exports/comparisons.
