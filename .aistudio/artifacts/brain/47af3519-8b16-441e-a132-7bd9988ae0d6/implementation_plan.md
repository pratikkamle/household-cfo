# Household CFO — Stability and End-to-End Reliability Plan

Comprehensive engineering plan to stabilize the Household CFO Android application on Android 16 (Oppo K13 Pro) and Nothing Phone 3a without discarding features or altering intended UI workflows.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were explicitly confirmed during the clarification interview:
> - **Codebase Baseline**: Keep the current `main` branch baseline (`b8fe342a0731ab0a654ad57550604e2efc3925a3`) and stabilize the existing files directly rather than performing another wholesale commit rollback.
> - **Observed Crash Point**: Crashes occur **immediately on app launch before any user interaction**, with secondary crashes during login/authentication on Oppo K13 Pro (Android 16) and Nothing Phone 3a.
> - **Diagnostics**: Implement an in-app **Diagnostic Crash Reporter** in debug builds to safely surface uncaught startup exceptions and stack traces in a non-intrusive sheet without exposing user passwords or sensitive financial numbers.

---

## 1. Executive Summary of Current Project Health

The Household CFO codebase is currently in a **non-compilable, unstable state** resulting from partial and inconsistent edits between commits `c0addaf` and `b8fe342`.

1. **Build Status**: **Broken (P0)**. Running `gradle :app:compileDebugKotlin` fails with 40+ compilation errors. Root causes include:
   - `app/build.gradle.kts` and `gradle/libs.versions.toml` were stripped of essential dependencies (Room compiler/KSP, Firebase plugins, Coroutines, Material Icons Extended).
   - `FinanceViewModel.kt` calls methods and properties on `FinanceRepository`, `AppDatabase`, `AuthScreen`, and `CloudSyncManager` that do not exist or have mismatched parameter signatures.
2. **Launch & Runtime Status**: **Fatal (P1)**. Even if assembled, the app crashes immediately upon process launch because:
   - `AndroidManifest.xml` lacks `android:name=".HouseholdCfoApp"`, meaning `HouseholdCfoApp.onCreate()` is never executed.
   - `CloudSyncManager` initializes `FirebaseAuth` and adds listeners during eager class construction before `FirebaseApp.initializeApp(context)` is guaranteed to run, throwing an unhandled `IllegalStateException`.
   - Android 16 (API 36) edge-to-edge window insets and missing `POST_NOTIFICATIONS` permission trigger system crashes on newer OEM skins (ColorOS/Oppo and Nothing OS).
3. **Architecture & Persistence Status**: **Fragile (P2)**. Room database currently specifies `version = 1` with `fallbackToDestructiveMigration()`. Mismatched entity definitions (`RecurringRuleEntity` vs `RecurringTransactionEntity`) and lack of atomic balance recalculations threaten financial ledger accuracy.

---

## 2. Confirmed Findings with Supporting Evidence

### Finding 1: Stripped Dependency Catalog & Build Blockers
- **Files**: `app/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`
- **Evidence**:
  - `libs.versions.toml` is missing `google-devtools-ksp`, `secrets`, `firebase-bom`, `firebase-auth`, `firebase-firestore`, `androidx-room-compiler`, `androidx-compose-material-icons-extended`, `coil-compose`, and `retrofit`.
  - `app/build.gradle.kts` lacks `ksp(libs.androidx.room.compiler)` and Firebase plugins.
  - `applicationId` was changed to `"com.example"`, creating a mismatch with `google-services.json` (`"package_name": "com.aistudio.householdcfo.mpzkvq"`).
- **Impact**: Instant compilation failure (`compileDebugKotlin FAILED`).

### Finding 2: Missing Application Class in Android Manifest
- **File**: `app/src/main/AndroidManifest.xml` (Line 8)
- **Evidence**: `<application>` does not declare `android:name="com.example.HouseholdCfoApp"`.
- **Impact**: `HouseholdCfoApp.onCreate()` never executes. Global Firebase initialization and `SmartNotificationManager.initialize()` are skipped.

### Finding 3: Eager Firebase Instantiation Race Condition (Immediate Launch Crash)
- **File**: `app/src/main/java/com/example/data/sync/CloudSyncManager.kt` (Lines 27–43)
- **Evidence**:
  ```kotlin
  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  init {
      auth.addAuthStateListener { ... } // Eagerly evaluated when CloudSyncManager is instantiated
  }
  ```
  `FinanceViewModel` instantiates `CloudSyncManager(application, database)` in its field initializer. When `MainActivity` initializes `viewModel by viewModels()`, `CloudSyncManager.init` executes before `MainActivity.onCreate()`'s try-catch block for `FirebaseApp.initializeApp(this)`.
- **Impact**: Throws `java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process` causing immediate process crash on startup.

### Finding 4: Method Signature and Type Mismatches across ViewModel and Repository
- **Files**: `FinanceViewModel.kt`, `FinanceRepository.kt`, `AppDatabase.kt`
- **Evidence**:
  - Line 69 of `FinanceViewModel.kt`: `AppDatabase.getDatabase(application)` called, but `AppDatabase.kt` only defines `AppDatabase.getInstance(context)`.
  - `FinanceViewModel.kt` calls `repository.getHousehold()`, `repository.getUsers()`, `repository.saveGoal()`, `repository.saveRecurringRule()`, `repository.clearAllData()`, `repository.resetDatabase()` — none of which exist in `FinanceRepository.kt` (they are named `getPrimaryHouseholdFlow()`, `getAllUsers()`, `insertGoal()`, etc.).
  - `RecurringRuleEntity` vs `RecurringTransactionEntity`: Types are confused across ViewModel and DAOs.
- **Impact**: 25+ fatal unresolved symbol compilation errors.

### Finding 5: AuthScreen and CloudSyncManager Parameter Disconnects
- **Files**: `MainActivity.kt`, `AuthScreen.kt`, `CloudSyncManager.kt`
- **Evidence**:
  - `MainActivity.kt` lines 132–145 invoke `AuthScreen` passing callbacks `onSignInWithEmail`, `onRegisterWithEmail`, `onSignInWithGoogle`, `onContinueOfflineAdmin`.
  - `AuthScreen.kt` line 32 was declared as `fun AuthScreen(viewModel: FinanceViewModel, modifier: Modifier = Modifier)`.
  - In `CloudSyncManager.kt`, `signInWithEmail` takes `(email, pass, onResult)` while ViewModel expected coroutine-scoped callbacks.
- **Impact**: Fatal compile failure preventing authentication screen from rendering.

### Finding 6: Missing Android 16 / Oppo Specific Permissions & Notification Registration
- **File**: `app/src/main/AndroidManifest.xml`
- **Evidence**:
  - Missing `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` (mandatory on Android 13+ / API 33+; both target devices are Android 15/16).
  - Missing registration for `FinanceNotificationListenerService` (`<service android:name=".service.FinanceNotificationListenerService" ...>`).
- **Impact**: SecurityException when scheduling credit card bill reminders or querying notification listeners on Android 16 (ColorOS 16 / Nothing OS 3).

---

## 3. Hypotheses Requiring Device Logs or Targeted Experiments

| Hypothesis | Suspected Root Cause | Investigation Test |
| :--- | :--- | :--- |
| **H1: Credential Manager Crash on Google Sign-In** | `GetCredentialRequest` throws unhandled `NoCredentialException` or `GetCredentialCancellationException` on Oppo K13 Pro when Google Play Services is delayed or non-responsive. | Unit test wrapping `CredentialManager.getCredential` with safe exception handling; logcat inspection during auth. |
| **H2: Android 16 Edge-to-Edge System Bar Overlap** | Android 16 enforces opt-out removal for edge-to-edge. `TopAppBar` or dialogs may render behind status bar cutouts on Oppo punch-hole displays. | Verify `WindowInsets.safeDrawing` and `WindowInsets.statusBars` in Compose `Scaffold`. |
| **H3: SQLite Destruction on Existing Device Installs** | `fallbackToDestructiveMigration()` in `AppDatabase` wipes data if schema version mismatches an earlier APK installed on the phone. | Test schema migration or version alignment to preserve local test databases. |

---

## 4. Prioritized Blockers

```
┌────────────────────────────────────────────────────────┐
│ P0: BUILD BLOCKERS                                     │
│  - Restore TOML dependencies and Gradle plugins        │
│  - Reconcile FinanceRepository, DAOs & ViewModel APIs  │
│  - Align AuthScreen callbacks and parameters           │
├────────────────────────────────────────────────────────┤
│ P1: LAUNCH & LOGIN CRASHES                             │
│  - Declare HouseholdCfoApp in AndroidManifest.xml      │
│  - Safe lazy FirebaseApp init before FirebaseAuth      │
│  - Add POST_NOTIFICATIONS permission for Android 16    │
│  - Safe Google Sign-In Credential Manager try-catch    │
├────────────────────────────────────────────────────────┤
│ P2: DATA INTEGRITY & FINANCIAL ACCURACY                │
│  - Atomic balance calculation in Account reconciliation│
│  - Unified RecurringTransactionEntity model            │
│  - Safe offline admin fallback persistence             │
├────────────────────────────────────────────────────────┤
│ P3: FEATURE VERIFICATION                               │
│  - Verify all 12 core user flows                       │
│  - Implement in-app Debug Crash Reporter UI            │
├────────────────────────────────────────────────────────┤
│ P4: RELEASE READINESS                                  │
│  - Robolectric test suite execution                    │
│  - APK assembly and verification                       │
└────────────────────────────────────────────────────────┘
```

---

## 5. File-by-File Change Plan

| File Path | Nature of Change | Justification & Expected Result |
| :--- | :--- | :--- |
| `gradle/libs.versions.toml` | **Update** | Re-add Version Catalog entries for Firebase, Room, KSP, Coroutines, Material Icons Extended, and Coil. |
| `build.gradle.kts` | **Update** | Register plugins `google.devtools.ksp`, `secrets`, `google.services`. |
| `app/build.gradle.kts` | **Update** | Re-enable Room KSP compiler, Firebase BOM, extended icons, coroutines, and restore `applicationId = "com.aistudio.householdcfo.mpzkvq"`. |
| `app/src/main/AndroidManifest.xml` | **Update** | Add `android:name=".HouseholdCfoApp"`, add `POST_NOTIFICATIONS` permission, and declare `FinanceNotificationListenerService`. |
| `app/src/main/java/com/example/HouseholdCfoApp.kt` | **Update** | Ensure safe, synchronized initialization of FirebaseApp and NotificationManager with robust try-catch. |
| `app/src/main/java/com/example/data/sync/CloudSyncManager.kt` | **Update** | Guard `FirebaseAuth.getInstance()` and `firestore` against early instantiation; wrap `CredentialManager` Google Sign-In with safe exception capture. |
| `app/src/main/java/com/example/data/local/AppDatabase.kt` | **Update** | Add alias `getDatabase(context)` to companion object matching `getInstance(context)` to prevent callsite breakage. |
| `app/src/main/java/com/example/data/repository/FinanceRepository.kt` | **Update** | Add all missing wrapper methods expected by `FinanceViewModel` (`getHousehold()`, `getUsers()`, `saveGoal()`, `saveRecurringRule()`, `clearAllData()`, `resetDatabase()`, `exportTransactionsToCsv()`). |
| `app/src/main/java/com/example/ui/viewmodel/FinanceViewModel.kt` | **Update** | Resolve parameter mismatches, standardize recurring rule entity usage, and wire safe offline login transition. |
| `app/src/main/java/com/example/ui/screens/AuthScreen.kt` | **Update** | Support both callback-based and ViewModel-based parameters so `MainActivity` can supply auth handlers cleanly. |
| `app/src/main/java/com/example/ui/components/DebugCrashReporter.kt` | **New** | In-app debug error banner/dialog that records uncaught launch/login exceptions safely for rapid on-device diagnosis. |
| `app/src/main/java/com/example/MainActivity.kt` | **Update** | Wrap Compose root with `DebugCrashReporter` in debug builds; ensure edge-to-edge complies with Android 16 WindowInsets. |

---

## 6. Automated and Manual Test Matrix

### Automated Test Cases (Robolectric / Local JVM)
1. **`AppStartupTest`**: Verifies `HouseholdCfoApp.onCreate()` and `FinanceViewModel` creation without Firebase throwing `IllegalStateException`.
2. **`DatabaseIntegrityTest`**: Verifies Room initialization, user/household insertion, and discretionary budget scaling.
3. **`FinancialCalculationTest`**: Asserts `MonthSummary` calculation correctly handles income, recurring rules, and discretionary deductions.
4. **`AuthScreenStateTest`**: Verifies transitioning between unauthenticated state and authenticated state (including offline mode).

### Manual Test Scenarios (Oppo K13 Pro & Nothing Phone 3a)
1. **Fresh Install Launch**: App opens to Auth screen with zero crash dialogs.
2. **Offline Mode Entry**: Tapping "Continue Offline as Admin" immediately transitions to `MainApp` with default household seeded.
3. **Google Sign-In Flow**: Tapping Google Sign-in launches Credential Manager bottom sheet; canceling does not crash the app.
4. **Process Recreation**: Changing system dark mode or rotating screen restores viewmodel state cleanly.
5. **Add Transaction & Balance Update**: Adding an expense deducts from account balance and updates monthly budget card in real time.

---

## 7. Proposed Implementation Sequence

- **Step 1 (Immediate Next Step)**: Fix dependencies and build catalog (`libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`) and restore `applicationId`. Validate with `gradle :app:dependencies`.
- **Step 2**: Reconcile `AppDatabase` and `FinanceRepository` API contracts with `FinanceViewModel`.
- **Step 3**: Fix `AuthScreen` signatures and `CloudSyncManager` Firebase/CredentialManager safety wrappers.
- **Step 4**: Update `AndroidManifest.xml` (add `.HouseholdCfoApp`, `POST_NOTIFICATIONS`, listener service).
- **Step 5**: Introduce `DebugCrashReporter` for debug builds and connect it in `MainActivity`.
- **Step 6**: Execute `compile_applet` and run `gradle :app:testDebugUnitTest` to verify clean build and automated tests pass.

---

## 8. Risks, Dependencies, and Data-Loss Precautions

- **Database Preservation**: We will not alter table primary keys or drop columns in `AppDatabase` to ensure user records remain valid.
- **Credential Safety**: No API keys or OAuth secrets will be hardcoded; BuildConfig secrets remain linked via `.env.example`.
- **No Feature Pruning**: All existing UI features (Household Hub, Sensitivity PIN, CFO Advisor, Reports, Goals) will be strictly preserved.

---

## 9. Definition of Done for Declaring the App Stable

1. `gradle :app:assembleDebug` builds cleanly with zero errors.
2. `gradle :app:testDebugUnitTest` passes 100% of test cases.
3. The app cold launches on Android 16 without startup crashes.
4. Both offline mode and Firebase authentication allow entry into the app without uncaught exceptions.
5. The debug diagnostic UI is active in debug mode to display any future runtime warnings cleanly.

---

### Recommended First Implementation Step
**Approve Step 1**: Restore the Gradle dependencies, plugins, and version catalog in `libs.versions.toml`, `build.gradle.kts`, and `app/build.gradle.kts`, and align `applicationId` to match `google-services.json`.
