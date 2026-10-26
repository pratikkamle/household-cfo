# Reverting Codebase to Commit ecc53ca (Household CFO)

Restore the complete Household CFO Android application back to commit `ecc53cab9df32da97c786dc10566c013ee560d8d`, bringing back the original UI, household management, and PIN security flows while retaining cloud and signing credentials.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification:
> - **Scope of Revert**: Full restore of the codebase to the state at commit `ecc53cab9df32da97c786dc10566c013ee560d8d`.
> - **Crash Handling Strategy**: Revert to `ecc53ca` cleanly without applying speculative crash fixes or revamps that alter the UI or features. Compilation and builds will be strictly validated.
> - **Credential Safety**: Maintain platform environment files (`app/google-services.json`, `metadata.json`, and keystore configs) so Firebase authentication and build signing remain functional.

## 1. Overview & Core Concept

- **What It Does**: Household CFO is a personal and shared family finance application designed for Pratik and Manasvi. It manages bank/credit accounts, cash transactions, loans, investments, category-based budgets, dynamic recurring bills, and AI financial insights.
- **Target Audience / Persona**: Household partners and family CFOs tracking joint finances with role-based member authority (Admin vs. Member).
- **Key Value**: Returns the app to its original, feature-complete state—restoring the custom Household Security PIN authentication dialogs, administrative member promotion guards, sensitivity reveal modal, and rich financial hub.

## 2. User Experience & Visual Design

- **Key User Flows**:
  - **Household Security PIN Verification**: Users set a dedicated numeric security PIN upon household registration. When viewing sensitive account balances or net worth metrics, tapping the eye/lock icon triggers the `SensitivityAuthDialog` with PIN verification and biometric support.
  - **Admin Elevation & Role Switching**: Promoting household members to Admin or switching active user personas in `HouseholdHubDialog` prompts for the household Security PIN, protecting administrative privileges.
  - **Database Reset & Sensitive Reports**: Resetting database tables in `ReportsScreen` requires Admin PIN verification before opening the confirmation dialog.
  - **Original Dashboard & Financial Hub**: Restores the original layout with custom overview cards, quick actions, account breakdown, and monthly budget progress.

- **Visual Identity & Theme**:
  - *Theme*: Modern Material 3 financial palette with high-contrast emerald/teal accents for surplus/income and clean coral/rose for expenses (`ExpenseRed`, `IncomeGreen`).
  - *Elevation & Components*: Elevated rounded cards (`RoundedCornerShape(12.dp)` to `16.dp`), clear numeric input keyboards (`KeyboardType.NumberPassword`), and distinct status chips for members and accounts.

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Full File Restoration from Commit `ecc53ca`**
  - *Chosen Approach*: Replace the modified files (`MainActivity.kt`, `SensitivityAuthDialog.kt`, `HouseholdHubDialog.kt`, `RegisterHouseholdDialog.kt`, `ReportsScreen.kt`, `FinanceViewModel.kt`) with their exact versions from commit `ecc53cab9df32da97c786dc10566c013ee560d8d`.
  - *Why*: Eliminates unwanted UI churn, restores discarded in-app PIN entry fields, and aligns the app 100% with the user's preferred build.
  - *Alternatives Considered*: Partial rollback was rejected based on the user's explicit preference for restoring the entire codebase to `ecc53ca`.

- **Decision 2: Preservation of Essential Container Configs**
  - *Chosen Approach*: Retain `app/google-services.json`, `metadata.json` (with `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`), and signing configuration in `build.gradle.kts` so that cloud connectivity and APK assembly remain intact.
  - *Why*: Modifying platform keystores or removing required capabilities breaks the build container.

- **Decision 3: Compilation and Stability Verification**
  - *Chosen Approach*: Run `compile_applet` immediately after synchronizing files to ensure all symbols, imports, and layouts build without error.
  - *Why*: Guarantees zero regressions or missing references while respecting the rule not to modify UI or feature behavior.

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Household CFO UI                     │
│  ┌───────────────────────┐   ┌───────────────────────┐ │
│  │    DashboardScreen    │   │  HouseholdHubDialog   │ │
│  │  - Net worth & tiles  │   │  - Persona switching  │ │
│  │  - Quick transactions │   │  - Member roles & PIN │ │
│  └───────────┬───────────┘   └───────────┬───────────┘ │
│              │                           │             │
│  ┌───────────▼───────────────────────────▼───────────┐ │
│  │         SensitivityAuthDialog (PIN & Biometric)   │ │
│  └───────────────────────┬───────────────────────────┘ │
└──────────────────────────┼─────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────┐
│                 FinanceViewModel                       │
│  - verifyHouseholdPin(pin)                             │
│  - updateMemberRole(userId, newRole, pin)              │
│  - updateHouseholdSecurityPin(newPin)                  │
│  - StateFlows: accounts, transactions, household, user │
└──────────────────────────┬─────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────┐
│               Data Persistence Layer                   │
│   Room Database (Local)  ◄──►  Firebase Firestore      │
└────────────────────────────────────────────────────────┘
```

- **Interactive State Transitions**:
  - `showSensitivityAuthDialog`: Triggers in-app PIN modal. Submitting the correct PIN reveals blurred amounts (`setSensitivityRevealed(true)`).
  - `userPendingAdminSwitch`: Prompts for Admin PIN before updating active user persona.
  - `memberPendingAdminPromotion`: Prompts for Household Security PIN before updating role to `ADMIN`.
