## Documentation
[Application Handbook] (docs/MZBotanicals_Application_Handbook.md)

Skip to content
Using Gmail with screen readers
Conversations
88% of 100 GB used
Terms · Privacy · Program Policies
Last account activity: 0 minutes ago
Open in 2 other locations · Details
MZBotanicals_Application_Handbook (1).md
100%

Markdown

Preview
MZBotanicals — Application Handbook
Version: 1.0 documentation draft • Prepared November 18, 2026 Owner: Zak Falkenbach • Repository: https://github.com/MajiinZ/MzbotanicalV1

1. Purpose and scope
MZBotanicals is a Kotlin Multiplatform commerce application intended for genetics businesses and seed banks. Its product direction combines a branded customer storefront with business administration across Android and desktop, with iOS planned.

This handbook provides a starting point for development, onboarding, demonstrations, support, and release decisions. It is based on the repository's main-branch README, MOCK_APPS.md, settings.gradle.kts, composeApp/build.gradle.kts, gradle/libs.versions.toml, firestore.rules, and root/module listings inspected on the preparation date. It is a documentation reconstruction, not a runtime audit. No application builds, payment tests, or deployed Firebase checks were performed.

The accessible repository metadata reports its last push on October 26, 2026. More recent local changes may therefore be absent. Existing prose and configuration are evidence of intent or configuration; neither proves a deployed feature works.

How to interpret status
Configured: directly visible in inspected configuration.
Documented: described in existing project notes, without runtime verification.
Planned: product direction; availability is not established.
To verify: requires current source, deployment details, or an end-to-end check.
2. Product and platform overview
Area	Evidence and status
Android	Configured in composeApp with Jetpack Compose/Compose Multiplatform.
Desktop	Configured JVM desktop target and installer formats. Availability on each operating system requires verification.
Browser	webApp module exists; composeApp delegates browser entry to it. Production readiness is unverified.
iOS	Planned. No iOS target appears in the inspected composeApp configuration.
Authentication	Firebase and Google sign-in are documented; deployed provider configuration needs verification.
Catalog, product details, cart, checkout, profile	Corresponding feature modules are registered. Complete behavior needs runtime verification.
Admin	Admin and product-management modules are registered; repository description refers to a mock admin workspace.
Payments	Existing notes describe a mock AeroPay flow associated with codex/mock-apps. No live payment capability is certified here.
Order emails	Claimed by the older README; backend implementation and delivery are unverified.
Multiple business customers	Business goal. Tenant provisioning and isolation have not been established by this review.
The historical name KillrsGenetics remains in documentation, package identifiers, and signing configuration. Use MZBotanicals for product descriptions. Changing an Android application ID is a separate migration decision, not a documentation rename.

3. Customer and business workflows
Customer walkthrough
Launch the app and follow any presented age gate. A gate's identity-verification strength has not been verified.
Sign in using an enabled provider.
Browse products and available categories.
Open product details, choose an available option and quantity, and add it to the cart.
Review cart contents and checkout delivery information.
Confirm whether the installed build uses demo checkout before demonstrating payment.
Review any confirmation and order history available in that build.
Use Profile to maintain supported customer details.
Exact screen labels should be checked against the latest app before distributing this section as a customer manual.

Demo checkout behavior documented in MOCK_APPS.md
The notes identify codex/mock-apps as the demo branch, although the notes themselves are also present on main. Verify the actual build branch and implementation before use.

In that documented demo, valid delivery details and a nonempty cart enable “Pay by bank with AeroPay.” The flow saves an account-specific local demo order and opens a success screen identifying the payment as a mock. It does not contact AeroPay, charge money, create a backend order, update stock, or clear the cart. Orders persist locally across restarts and do not sync across devices. Repeated taps are documented as saving one order per checkout visit.

Never treat a local demo order or a client success screen as payment confirmation or a fulfillment request.

Business onboarding checklist
Before onboarding a paying business, record its branding, catalog owner, authorized administrators, product data, support contact, payment arrangement, fulfillment procedure, and supported platforms. Establish whether the deployment is a separate branded installation or a shared tenant. Validate isolation before putting separate businesses' customer data into a shared environment.

Commercial pricing should live in an editable proposal or agreement. The previously discussed $1,500 setup plus $199/month is a proposed offer, not an application capability or a verified contract.

4. Architecture and project map
The project uses Gradle modules and Kotlin Multiplatform source sets. The following responsibilities combine inspected module names with the older README; they are an orientation map rather than a class-level dependency audit.

Path	Responsibility / inspection boundary
composeApp/	Android and desktop application configuration, shared Compose UI dependencies, platform entry points.
shared/	Shared models, utilities, and core logic according to the README.
data/	Data-layer module; implementation not inspected in this reconstruction.
di/	Dependency-injection module; Koin appears in application dependencies.
navigation/	Navigation module shared by the application.
feature/auth/	Authentication feature module.
feature/home/	Home and nested cart, categories, search, checkout, payment-completed, and product-overview modules.
feature/details/	Product-details feature module.
feature/profile/	Profile feature module.
feature/admin/	Admin and nested manage_product modules.
desktopFirebase/	Desktop Firebase module; exact internal API remains to be documented.
server/	Backend module described as Ktor by the README. Routes and deployment are unverified.
webApp/	Browser module.
firestore.rules	Checked-in Firestore authorization rules; deployed rules may differ.
gradle/libs.versions.toml	Shared dependency and plugin version catalog.
The settings file repeats the checkout include several times and includes shared twice. These are cleanup candidates, not proof of a build failure.

Configured toolchain
Item	Inspected value
Kotlin	2.1.21
Android Gradle Plugin	8.8.2
Compose Multiplatform	1.8.0
Ktor version catalog entry	3.0.3
Coroutines	1.10.1
Koin version catalog entry	4.1.0-Beta11
Android minimum / target / compile SDK	26 / 35 / 35
Android application ID and namespace	org.mz.killrs
Android version name / code	1.0 / 1
Android JVM bytecode target	11
Desktop package version	1.0.1
These are project values, not recommendations to upgrade or claims of current store eligibility. Gradle's runtime JDK requirement is separate from bytecode target 11.

5. Developer setup
Prerequisites and configuration
Obtain repository access, an Android development environment compatible with the pinned build, Android SDK 35, and a compatible Gradle runtime JDK. Use the checked-in Gradle wrapper rather than a separately installed Gradle version.

Clone the repository, open its root in Android Studio, and allow Gradle synchronization. Configure the local Android SDK path as required. Obtain the correct Firebase client configuration from the project owner and place it at composeApp/google-services.json. The build explicitly reads that path for desktop Firebase configuration. The similarly named root file composeAppgoogle-services.json is not that configured input.

The Firebase Android client must match org.mz.killrs. Confirm the enabled authentication providers, applicable signing certificate fingerprints, Firestore rules, and Storage rules in the intended environment. Keep production service-account credentials and signing secrets outside source control and client distributions.

Build commands to validate locally
Run from the repository root. These commands follow the inspected module/target names but were not executed for this handbook. On Windows, use gradlew.bat.

./gradlew --version
./gradlew :composeApp:tasks --all
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:run
The desktop resource task generates firebase-desktop.properties from composeApp/google-services.json. Missing or mismatched client configuration can therefore block desktop resources as well as Android Firebase setup.

Use the task listing to select available tests, installers, and web tasks. Do not assume every installer can be built on every host operating system.

Release signing
The Android build looks for ~/.android/killrsgenetics-upload.properties containing storeFile, storePassword, keyAlias, and keyPassword. Treat this file and its referenced keystore as secrets. If the properties file is absent, the inspected release configuration does not select that signing config. Verify signing before distribution; a successful build alone is insufficient.

6. Data and authorization
This section summarizes the checked-in rules, not a complete database schema. Exact model types, indexes, migrations, and Storage rules still need documentation.

Collection	Checked-in rule behavior
users/{uid}	Owner/admin read; owner creates a restricted profile and updates displayName only; admin deletes.
customer/{uid}	Owner/admin direct get; owner creates matching id with isAdmin false; owner updates an allowlist of profile/cart fields; admin deletes.
product/{productId}	Admin reads all; other reads require isPublished true; admin writes. Customer list queries must respect the publication filter.
products/{productId}	Public reads and admin writes; retained as a legacy collection.
orders/{orderId}	Signed-in owner identified by userId, or admin, may read; client writes are denied.
All other paths	Reads and writes denied by the fallback rule.
Admin authorization currently uses one verified email address hard-coded in firestore.rules. A profile isAdmin field does not grant those privileges. This scheme does not establish roles for multiple business tenants.

The singular product and legacy plural products collections have different read policies. Record which collection each deployed feature actually uses before migration or cleanup.

Profile-photo notes describe Firebase Storage objects under profile_images/

7. Backend and payment documentation to complete
No endpoint contract was verified. Do not infer endpoint names or request bodies from screen names. For each actual endpoint, capture:

Field	Required content
Endpoint	Method, path, purpose, and environment base URL.
Authentication	Token type, validation, expiry handling, and authorization.
Contract	Request fields, validation, response fields, and safe example payloads.
Failures	Status codes, user-facing behavior, timeout and retry rules.
Side effects	Order creation, inventory changes, notifications, and audit records.
Duplicate handling	Idempotency keys and payment-webhook replay behavior where relevant.
Before live checkout, verify server-authoritative pricing, stock checks, payment verification, duplicate prevention, durable order creation, notification delivery, fulfillment, refunds, and reconciliation. Record the provider and environment once confirmed. An older README's planned provider names are not evidence of an active integration.

8. Test and release checklist
Record the build version, commit, environment, device/OS, date, result, and evidence for each check. None are marked passed by this handbook.

Clean installation, upgrade, cold launch, branding, portrait and landscape layout.
Sign-in, cancellation, sign-out, session restoration, and recoverable provider errors.
Published products visible; unpublished products unavailable to customers.
Product options, quantities, cart totals, empty states, and failed requests.
Correct checkout validation and clear demo/live identification.
Real payment success, failure, cancellation, delayed confirmation, and retries if live checkout exists.
One durable order and one intended stock adjustment per confirmed purchase.
Correct receipt, fulfillment status, refund handling, and customer history.
Another user cannot access profiles, orders, or photos; unauthorized users cannot administer products.
Separate businesses cannot access each other's data if tenancy is supported.
Profile edits and photo changes survive restart and preserve prior data on failure.
Account-deletion request, identity verification, completion, and any documented retention behavior.
Offline recovery, accessibility, long text, keyboard behavior, and desktop sizing.
Signed artifacts, version increments, support contact, privacy disclosures, and rollback plan.
Policy documents should reflect actual data handling and operating arrangements. This handbook supplies no legal certification and does not repeat the older README's claims of legal compliance as established facts.

9. Support and operations
Symptom	First investigation
Google sign-in fails	Verify app ID, Firebase provider setup, client config, and the installed build's signing fingerprints. Capture the exact error.
Product list is empty	Check environment, connectivity, collection name, publication status, and query/rule alignment.
Profile photo fails	Check Storage availability, authorization, Firestore update permission, and connectivity.
Checkout succeeds but no backend order exists	Identify the build branch; the documented demo deliberately saves only local history.
Order is absent on another device	Local demo history is documented as device-local. Confirm whether a server order exists.
Desktop resource generation fails	Check composeApp/google-services.json and a matching org.mz.killrs client.
For an incident, capture the version/commit, platform, time, steps, expected result, actual result, and sanitized logs. Avoid collecting passwords, tokens, full payment details, or unnecessary customer data. Do not request payment retries until duplicate or delayed charge risk is understood.

Operational details still to record: production/staging ownership, deployment host, configuration inventory without secret values, monitoring destination, backup and restore procedure, rollback steps, support email, and escalation contact.

10. Keeping documentation useful
Update this handbook whenever a feature, data model, integration, permission, build requirement, or customer workflow changes. Keep customer instructions aligned with the shipped build.

For a larger team, split this content into README.md plus docs/SETUP.md, ARCHITECTURE.md, DATA_AND_SECURITY.md, API.md, USER_GUIDE.md, OPERATIONS.md, and RELEASE_CHECKLIST.md. Start with this single handbook until those sections need independent ownership.

Next verification priorities
Compare the current local app with the accessible GitHub snapshot.
Verify the installed build's checkout implementation and backend order lifecycle.
Inspect model classes, repositories, server routes, and navigation to replace remaining orientation notes with precise contracts.
Confirm business separation, admin provisioning, account deletion, and operational ownership.
Run and record the release checks above.
Source index
All paths refer to the inspected main branch of MajiinZ/MzbotanicalV1:

README.md — historical positioning, architecture descriptions, and roadmap.
MOCK_APPS.md — demo-branch checkout behavior, photos, order history, and manual checks.
settings.gradle.kts — registered modules.
composeApp/build.gradle.kts — platforms, package IDs, signing, desktop configuration.
gradle/libs.versions.toml — pinned versions.
firestore.rules — checked-in access policies.
Root and server/data/navigation listings — module presence only.
This document has not been committed to the repository and makes no changes to application behavior.

Displaying MZBotanicals_Application_Handbook (1).md.

## Analytics (Amplitude)

The Android app sends product analytics to Amplitude through `org.mz.killrs.shared.analytics.Analytics`.

| Event | When it fires | Properties |
|---|---|---|
| `Sign Up` | First Google sign-in that creates a new customer record | `method` |
| `Activated` | First successful add-to-cart for a user (once per user per device) | `trigger`, `product_id`, `quantity` |
| `Return Visit` | App comes to the foreground on a later local calendar day than the last open | `days_since_last_visit` |
| `Subscribed` | Not wired yet. The app has no subscription flow. Call `Analytics.subscribed(...)` once Stripe confirms a subscription | `plan`, `price_usd`, `billing_interval` |

Signed-in users are identified by their Firebase UID, and sign out resets the Amplitude user. Amplitude's default session tracking (`[Amplitude] Start Session` / `End Session`) is also on.

### Setup

1. In Amplitude, open **Settings → Projects → default** and copy the project's **API Key**.
2. Add it to `local.properties` in the repo root (this file is gitignored):

   ```properties
   AMPLITUDE_API_KEY=your_key_here
   ```

   CI can set the `AMPLITUDE_API_KEY` environment variable instead.
3. Build and run the Android app. With no key, analytics stays off and the app behaves exactly as before.

Desktop builds use a no-op tracker.
