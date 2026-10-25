# Household CFO

Household CFO is a Kotlin Android app for managing a household's finances from a single place. It combines budgeting, account tracking, recurring bills, reports, member-based planning, and cloud-backed household sync into a mobile-first personal finance dashboard.

The app is designed around a shared family workflow: members can be added to a household, each person can track spending and income, and the household admin can manage security, budgets, and shared financial goals.

## Overview

This project was built as a private household finance management app and currently includes:

- Multi-member household setup with roles and permissions
- Monthly budgeting and category planning
- Income and expense entry for each household member
- Recurring commitment tracking and paid-status workflows
- Account tracking for checking, savings, cash, and credit cards
- Goal tracking and CFO-style financial review insights
- Smart parsing for SMS / notification-based transaction capture
- Firebase-backed authentication and household sync
- Notification-driven reminders for credit-card bill due dates

The current implementation is centered around Jetpack Compose UI, Room persistence, a repository layer, ViewModel state management, and Firebase for user authentication and data synchronization.

## Core features

### Household membership and permissions
- Household registration and admin creation flow
- Invite-code based household join flow
- Member management, role assignment, and active-user switching
- Household-level security PIN and sensitive-data protection
- Shared household identity and profile state across app sessions

### Monthly financial dashboard
- Monthly summary for income, scheduled commitments, discretionary spend, and available cash flow
- Filter by month and by household member
- Dashboard cards and analytics for spending patterns
- Display of member-level spending breakdown and household budget utilization

### Transactions and recurring schedules
- Add income and expense entries with merchant, category, payment method, notes, and account linkage
- Expense classification into need / want / savings / investment categories
- Recurring rules for monthly or scheduled commitments
- Mark recurring items as paid and record them into the monthly ledger
- Transaction editing and deletion flows

### Accounts and affordability tracking
- Account management for checking, savings, cash, credit cards, and other account types
- Sensitive account balance controls behind household PIN or device auth
- Credit-card reminder logic to notify users about upcoming payment due dates
- Account-linked financial planning and balance reconciliation

### Budgets and planning
- Category-based budget plan creation and maintenance
- Household discretionary budget tracking
- Budget plan updates from the app UI
- Budget import/repair logic to keep totals aligned with household budget totals

### Goals, insights, and CFO review
- Savings goal creation with target amount, timeline, and progress tracking
- Goal contribution updates and completion toggling
- CFO-style financial report generation based on monthly summary and goal trends
- Goal feasibility and simulation logic for planning improvements

### Smart parsing and AI assistance
- Smart detection for transaction text extracted from SMS or notification messages
- Merchant/category inference based on spending patterns and transaction text
- Gemini / Firebase AI integration for financial guidance and intelligent features
- AI-assisted finance and household insight workflows

### Cloud sync and auth
- Firebase Auth support with Google sign-in and credential-based authentication
- Firestore / Firebase-backed household sync and restore flow
- Realtime sync setup and app state restoration after reinstall or sign-in
- Shared household publication and cloud recovery for local-first app usage

## Tech stack

- Kotlin
- Android Jetpack Compose
- Material 3 UI components
- Room Database
- AndroidX lifecycle and ViewModel architecture
- Kotlin Coroutines and Flow
- Firebase Authentication
- Firebase Firestore / Firebase services
- Firebase AI / Gemini API support
- Secrets Gradle plugin for environment configuration
- Gradle Kotlin DSL

## Repo structure

```text
household-cfo/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   ├── model/
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── sync/
│   │   │   │   ├── service/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/
│   │   │   │   │   ├── screens/
│   │   │   │   │   ├── theme/
│   │   │   │   │   └── viewmodel/
│   │   │   │   ├── util/
│   │   │   │   ├── HouseholdCfoApp.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   └── res/
│   │   ├── androidTest/
│   │   └── test/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradle/
├── settings.gradle.kts
├── .env.example
├── firebase.json
├── firebase-blueprint.json
├── firestore.rules
├── README.md
├── metadata.json
├── .gitignore
├── gradlew
├── gradlew.bat
└── LICENSE (if added later)
```

## Architecture summary

The app follows a clean layered design:

- UI layer: Compose screens and reusable components in `app/src/main/java/com/example/ui`
- ViewModel layer: state management and app flows in `FinanceViewModel`
- Repository layer: local database and sync logic in `FinanceRepository`
- Data layer: Room DAOs and entities in `com.example.data.local` and `com.example.data.model`
- Service layer: notification and transaction-capture utilities in `com.example.service`
- Sync layer: Firebase and household sync logic in `com.example.data.sync`

Key implementation files:

- `MainActivity.kt` — app entry point and screen switching shell
- `HouseholdCfoApp.kt` — app-level Firebase initialization
- `FinanceViewModel.kt` — main state holder for month summaries, transactions, members, budgets, goals, sync, and actions
- `FinanceRepository.kt` — repository APIs for transactions, households, users, budgets, goals, and account updates
- `CloudSyncManager.kt` — cloud sync logic for user/household data
- `SmartTransactionParser.kt` — parses text-based transaction inputs into structured entries
- `CreditCardBillReminderHelper.kt` — sends bill-due reminders based on card schedules

## Setup and configuration

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 11 or newer
- Android SDK with API 36 support
- Firebase project configured for Android
- Optional Gemini / Firebase AI API key for AI-powered features

### 1. Clone the project

```bash
git clone https://github.com/pratikkamle/household-cfo.git
cd household-cfo
```

### 2. Configure local environment values

The project uses the Secrets Gradle plugin and reads from `.env` by default.

```bash
cp .env.example .env
```

Example values:

```dotenv
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

If you are not using Gemini in a local build, you can leave the key blank or provide a placeholder for the build to remain valid.

### 3. Configure Firebase

This app expects a Firebase project to be configured:

- Add your Android app package based on the app ID in `app/build.gradle.kts`
- Download `google-services.json` and place it in the `app/` directory
- Enable the required Firebase products for the app (Auth, Firestore, App Check if configured)

The project is already set up to use the Google Services plugin and Firebase dependencies, but the actual project credentials must be added in your local environment or Firebase setup.

### 4. Open in Android Studio

- Open the repository in Android Studio
- Let Gradle sync finish
- Make sure the Android SDK is installed and available
- Build the project

### 5. Run the app

From Android Studio:

- Select an emulator or a connected Android device
- Press Run

Or from the command line:

```bash
./gradlew assembleDebug
```

## Tests

The project already contains basic unit and Robolectric-based tests.

Run:

```bash
./gradlew test
```

For instrumentation tests:

```bash
./gradlew connectedDebugAndroidTest
```

## Typical usage

### Create a household
- Register a household admin
- Set the household name and discretionary budget
- Configure a security PIN
- Add family members or join via invite code

### Add transactions
- Record income and expenses from the dashboard
- Set category, account, payment method, merchant, and notes
- Mark whether an expense is discretionary, a recurring commitment, or an investment

### Manage budgets
- Assign category budgets
- Review monthly budget progress vs. actual spending
- Adjust budget plans from the household budget UI

### Review reports
- Use the dashboard summary and reports screen to see spending trends
- Check cash flow, savings rate, and household financial health
- Track goals and understanding of upcoming obligations

### Use smart parsing
- Parse text from SMS or app notifications into a suggested transaction
- Confirm or reject suggested entries before saving

### Stay on top of bills
- Use credit card reminder logic to warn about approaching due dates

## Data model highlights

The app uses Room entities such as:

- `HouseholdEntity`
- `UserEntity`
- `TransactionEntity`
- `RecurringTransactionEntity`
- `AccountEntity`
- `CategoryBudgetEntity`
- `FinancialGoalEntity`

These entity models are defined in the `app/src/main/java/com/example/data/model` package and are used by the repository and UI layers.

## Security and privacy notes

This is a household financial app and includes sensitive account and spending data. The current implementation includes:

- Household-level security PIN
- Sensitive data masking behind auth flows
- Local Room storage plus cloud sync
- Authentication for user identity and household membership

For production deployment, review:

- Firebase security rules
- App Check configuration
- Notification permission handling
- Proper Android release signing and keystore management

## Current project status

This repository is a functional Android app prototype / development project with a substantial feature set for household budget tracking, planning, and reporting. It is organized around a mobile-first, local-first architecture but includes cloud sync and AI-assisted features to support a richer household finance workflow.

## Future improvements

Possible enhancements include:

- Better CSV / export and reporting sharing features
- More advanced AI-generated spending analysis
- Improved cross-household switching and user management
- Additional finance learning tools for debt reduction and savings optimization
- Stronger release automation and CI checks
- Broader test coverage for the ViewModel and sync flows

## License

No explicit project license was found in the repository metadata at the time of review. If this project is intended to be publicly reusable or distributable, consider adding an open-source license such as MIT or Apache 2.0.

## Contributing

If you want to contribute:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and validate your build
5. Open a pull request with a clear summary of the improvement

## Contact

Repository owner: `pratikkamle`
Project: `household-cfo`

This README reflects the current repository structure and the features implemented in the app as of the latest repository state.
