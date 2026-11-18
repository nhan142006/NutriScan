# Milestone 05 — Production & Security, NutriScan Group 04

This update is based on current main `c11cab7`, not the old `2b06278` candidate.
Application version: **2.0.1**, versionCode **3**, package `vn.edu.vlu.milestone2`.
The final generated PDF, hashes and actual source revision are packaged in `submission/`.

## Scope and design

- Release: `isMinifyEnabled = true`, `isShrinkResources = true`, optimized platform rules plus `app/proguard-rules.pro`.
- Keep rules target Retrofit proxy contracts, annotated Gson DTOs and stable Room JSON field names. Room/Glide/Firebase also provide consumer rules.
- Signing properties come from `.signing/release.properties`, `local.properties` or `NUTRISCAN_RELEASE_*` environment variables. Release refuses to build without private signing configuration. No debug-key fallback.
- Gemini and TheMealDB configuration is injected through BuildConfig from untracked local properties. BuildConfig prevents source-control leakage; it does **not** make client API keys unextractable from an APK. No HTTP URLs or tokens are logged by this app's OkHttp configuration.
- `EncryptedSessionStore` uses Keystore-backed AES-256-GCM and AES-256-SIV encrypted preferences. The ID token cache is checked against the current UID and both Firebase expiry and a maximum 5-minute age. It is used by authenticated Firestore REST profile reads. Firestore SDK writes retain their offline queue.
- Sign-out clears the cache on I/O before Firebase sign-out; SDK sign-out still occurs if encrypted storage fails. No password is persisted. SDK-managed session persistence is separate and is not claimed to have been replaced by this cache.
- Cloud backup/device transfer exclude preferences, databases, private files and external app data. Cleartext traffic is disabled.
- EncryptedSharedPreferences is deprecated in current AndroidX, but is isolated behind `SessionTokenStore` because it is an explicit course requirement. It can be replaced without changing the auth/UI contracts.

## Performance work

Product, recipe and history rows use ListAdapter/DiffUtil and Glide memory/disk caches.
ViewHolder click listeners are installed once and reference only the currently bound item.
Recycling clears both model and image references. Fragment destruction detaches RecyclerView adapters and clears detail images/bindings.
Repository JSON/Room mapping runs on I/O via `withContext`/`flowOn`; Retrofit, Room and image compression remain off the UI thread.
LeakCanary exists only in Debug. The signed optimized Release is shell-profileable for genuine frame measurements, not debuggable.

## Reproduce verification

```text
gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --max-workers=2
gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:lintRelease --max-workers=2
gradlew.bat :app:connectedDebugAndroidTest --max-workers=2
gradlew.bat :app:assembleRelease :app:bundleRelease --max-workers=2
```

`CachedSessionTokenTest` checks account switching, expiry, blank tokens and clock rollback.
`ProfileCloudSyncTest` verifies the authenticated REST contract with MockWebServer and local fallback.
`EncryptedSessionStoreTest` verifies real encryption on the device, persistence across instances and clear-on-logout.
`Milestone05PerformanceTest` adds **test-only**, explicitly named fixtures: 80 products and 40 recipes. It also checks that destroyed product views are collectible after ten detail round trips (requires a signed-in test account).

`scripts/verify_milestone05.py` collects screenshots, meminfo, optional heap dumps, and product/recipe frame statistics. Credentials are supplied through `NUTRISCAN_TEST_EMAIL` / `NUTRISCAN_TEST_PASSWORD`, never committed in the script. Emulator fixture measurements isolate app rendering from unreliable remote APIs; they are not represented as live food-service data or a guarantee of zero leaks on every device.

## Release and submission

Run `scripts/New-ReleaseKeystore.ps1` **once**, then securely back up `.signing/`. Do not delete/regenerate the key for subsequent releases. The old downloaded APK used a different test key; an existing install signed with that key cannot be updated with the new key.

Run `scripts/Package-Milestone05.ps1`. Keep the APK, AAB, public certificate, SHA-256 checksums, final PDF, AI disclosure, demo and verification evidence together. Never submit `.signing/`, passwords, local.properties, raw auth tokens or a user database.

GitHub publication/tag creation is the final external step and requires an explicit fresh choice of `trithanh1577` or `Ngan3112`. Do not treat a link to `/releases/new` as a published Release.

Historical WBS evidence must retain the distinction between baseline code and new M05 work. Source commit logs show Gia Thanh's UI/data/test integration, Thành Trí's Room/Auth work and Hoàng Nhân's details/network integration. Do not assign unrecorded M05 work to another member without confirmation.

## References

- [Android R8 optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)
- [EncryptedSharedPreferences and backup restriction](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
- [APK signature verification](https://developer.android.com/tools/apksigner)
