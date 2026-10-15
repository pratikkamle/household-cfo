# Household CFO

Household CFO is a Kotlin-based Android finance management app designed for households to track spending, recurring bills, monthly budgets, accounts, and long-term goals with a shared family view. It combines local Room persistence with cloud sync and AI-assisted finance workflows, giving each household a single dashboard for planning and control.

## Overview

This app helps families and households:

- Track income and expenses by member or household
- Manage recurring commitments and scheduled payments
- Set category-based budget plans
- Review spending and available cash flow by month
- Track account balances and linked financial accounts
- Collaborate across household members with roles and permissions
- Use AI-powered financial suggestions and smart parsing for transactions
- Sync household data through Firebase-backed services

The project is built with Jetpack Compose and is structured around a single application module with Room-based data access, repository patterns, view models, and screen-level UI components.

## Key Features

### Dashboard and monthly financial visibility
- Monthly household summary with income, scheduled expenses, discretionary spend, and balance
- Member-by-member spending breakdown
- Budget and remaining discretionary calculations
- Navigation across Dashboard, Scheduled Commitments, Budget, Reports, Accounts, and Transactions

### Household management
- Create a household and assign an admin
- Invite users and manage member roles
- Join households using invite codes
- Maintain household-level security PIN and sensitive data visibility controls

### Transaction tracking
- Add income and expense entries
- Categorize transactions by nature and payment method
- Link transactions to accounts and members
- Edit and delete existing entries
- Support recurring transactions and mark them as paid

### Budgeting and planning
- Set monthly category budgets
- View budget totals against household spending
- Manage discretionary budget and category allocations
- Adjust household budget plans from a dedicated dialog

### Financial goals and advisory insights
- Create savings goals with target amounts and current progress
- Track completion states and contributions
- Simulate goal feasibility and expected timelines
- Generate CFO-style insights using summary metrics and household data

### Smart parsing and AI support
- Parse SMS/notification text into transaction suggestions
- Auto-detect financial patterns from messages and receipts
- Use Gemini / Firebase AI tooling for financial guidance and summaries
- Smart detection workflow for fast expense/income capture

### Cloud sync and account auth
- Firebase integration for authentication and household sync
- Google sign-in support
- Household data publishing and restoration across devices
- Realtime sync behavior for household records

## Tech Stack

- Kotlin
- Android Jetpack Compose
- Android Architecture Components
- Room Database
- Coroutines and Flow
- Firebase Authentication
- Firebase Firestore / Firebase services
- Google AI / Gemini API integration
- Gradle Kotlin DSL
- Material 3 design system

## Project Structure

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
│   └── proguard-rules.pro
├── build.gradle.kts
├── gradle.properties
├── gradle/
├── settings.gradle.kts
├── .env.example
├── firebase.json
├── firestore.rules
├── firebase-blueprint.json
├── metadata.json
├── .gitignore
├── README.md
└── gradlew / gradlew.bat
```

## Core Architecture

The application follows a layered architecture:

- UI layer: Compose screens and reusable components
- ViewModel layer: state management, app flows, and actions
- Repository layer: access to local database and sync orchestration
- Data layer: Room DAOs, entities, and data model objects
- Service layer: notification capture and background financial processing
- Sync layer: cloud synchronization logic with Firebase-backed household state

### Main application flow

- `MainActivity` hosts the app shell and branches between authentication and the app dashboard
- `FinanceViewModel` owns most state, including household, users, transactions, budgets, goals, and UI actions
- `FinanceRepository` exposes database operations and reconciliation helpers
- `AppDatabase` is the local persistence layer using Room
- `CloudSyncManager` manages cloud state and real-time household sync

## Setup and Installation

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 11+
- Android SDK with API 36 support
- A Firebase project (for Auth / Firestore / sync features)
- Optional: Gemini API key for AI features

### 1. Clone the repository

```bash
git clone https://github.com/pratikkamle/household-cfo.git
cd household-cfo
```

### 2. Configure environment values

Copy the sample environment file:

```bash
cp .env.example .env
```

Then update the values as needed. The sample includes:

```dotenv
GEMINI_API_KEY=MY_GEMINI_API_KEY
```

Note: this app is configured to use the Secrets Gradle Plugin and expects `.env` values at build time.

### 3. Configure Firebase

This app uses Firebase for authentication and sync features. You will need to:

- Create a Firebase project
- Add the Android app package matching the configured applicationId
- Download `google-services.json` and place it in the `app/` directory
- Enable the services required by the app (Auth, Firestore, App Check if configured)

The project is already configured to use Google services with the plugin, but the actual Firebase credentials must exist in your local environment or project setup.

### 4. Open in Android Studio

- Open the project in Android Studio
- Let Gradle sync complete
- Accept any SDK license prompts if required
- Build the project

### 5. Run the app

From Android Studio:

- Select an emulator or connected device
- Click Run

Or from command line:

```bash
./gradlew assembleDebug
```

## Running Tests

The project contains basic unit tests and Robolectric-based test scaffolding:

```bash
./gradlew test
```

Optional instrumentation tests can also be run through Android Studio or Gradle.

## Usage Highlights

### Create a Household
- Register a household admin
- Set a household name and discretionary budget
- Choose a security PIN
- Invite family members or join using an invite code

### Track Spending
- Record expenses for a specific member
- Assign category, account, payment method, and merchant details
- Use smart detection for SMS or notification parsing

### Review the Month
- View totals for income and discretionary spend
- Inspect budget balances and spending breakdowns by member
- Monitor recurring commitments and upcoming bills

### Manage Household Goals
- Add a savings goal
- Set a target, current savings, and duration
- Monitor progress and feasibility recommendations

## Data Model Summary

The app stores household finance data using Room entities such as:

- `HouseholdEntity`
- `UserEntity`
- `TransactionEntity`
- `RecurringRuleEntity`
- `AccountEntity`
- `CategoryBudgetEntity`
- `FinancialGoalEntity`

These model classes live under `app/src/main/java/com/example/data/model` and are used by the repository and ViewModel layers for calculation and UI rendering.

## Security Notes

This application contains personal finance data and includes:

- Household-level PIN protection
- Sensitive balance visibility controls
- Local storage and sync of financial records
- Authentication flows for user identity management

Use proper Firebase security rules and local device protections if you deploy this beyond local development.

## Known Project Characteristics

This repository appears to be a private, app-focused household finance dashboard with Android UI and local-first data persistence. The project is currently designed around a single household concept with a small multi-member structure and a premium finance dashboard style.

## Roadmap Ideas

Possible future improvements include:

- Better export/report sharing features
- More advanced AI-generated spending insights
- Multi-household switching and better tenant setup
- Cloud backup restoration UX cleanup
- Improved test suite coverage
- Stronger security policies and release signing automation

## License

This repository does not currently declare a license in the project metadata provided. If this project is intended for public distribution, consider adding an appropriate open-source license such as MIT or Apache 2.0.

## Contributing

If you want to contribute:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run the relevant tests
5. Open a pull request with a clear description of the feature or fix

## Contact / Maintainer

Repository owner: `pratikkamle`
Project name: `household-cfo`

## Final Note

Household CFO is designed as a family-first personal finance tracker with built-in budgeting, AI assistance, and household coordination. The application aims to blend pragmatic budgeting tools with a modern mobile UX and cloud-ready architecture for shared household decision-making.

---

This README is intended to serve as a practical project overview for onboarding, local setup, and feature understanding for future contributors and users.
