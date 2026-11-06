# Codebase Map

Generated: 2026-11-06T01:41:26Z | Files: 223 | Described: 0/223
<!-- gsd:codebase-meta {"generatedAt":"2026-11-06T01:41:26Z","fingerprint":"cc025e9e3566f03f9816e662c2d9195a1df99e28","fileCount":223,"truncated":false} -->

### (root)/
- `.gitignore`
- `AndroidUsageControl.code-workspace`
- `build.gradle.kts`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `PROJECT.md`
- `README.md`
- `settings.gradle.kts`

### .archive/planning/
- `.archive/planning/config.json`
- `.archive/planning/PROJECT.md`
- `.archive/planning/REQUIREMENTS.md`
- `.archive/planning/ROADMAP.md`
- `.archive/planning/STATE.md`

### .archive/planning/codebase/
- `.archive/planning/codebase/ARCHITECTURE.md`
- `.archive/planning/codebase/CONCERNS.md`
- `.archive/planning/codebase/CONVENTIONS.md`
- `.archive/planning/codebase/INTEGRATIONS.md`
- `.archive/planning/codebase/STACK.md`
- `.archive/planning/codebase/STRUCTURE.md`
- `.archive/planning/codebase/TESTING.md`

### .archive/planning/phases/01-foundation/
- `.archive/planning/phases/01-foundation/01-01-PLAN.md`
- `.archive/planning/phases/01-foundation/01-01-SUMMARY.md`
- `.archive/planning/phases/01-foundation/01-02-PLAN.md`
- `.archive/planning/phases/01-foundation/01-02-SUMMARY.md`
- `.archive/planning/phases/01-foundation/01-RESEARCH.md`
- `.archive/planning/phases/01-foundation/01-VERIFICATION.md`

### .archive/planning/phases/02-permission-walkthrough/
- `.archive/planning/phases/02-permission-walkthrough/02-01-PLAN.md`
- `.archive/planning/phases/02-permission-walkthrough/02-01-SUMMARY.md`
- `.archive/planning/phases/02-permission-walkthrough/02-02-PLAN.md`
- `.archive/planning/phases/02-permission-walkthrough/02-02-SUMMARY.md`
- `.archive/planning/phases/02-permission-walkthrough/02-RESEARCH.md`
- `.archive/planning/phases/02-permission-walkthrough/02-VERIFICATION.md`

### .archive/planning/phases/03-welcome-tour-and-quick-setup/
- `.archive/planning/phases/03-welcome-tour-and-quick-setup/03-01-PLAN.md`
- `.archive/planning/phases/03-welcome-tour-and-quick-setup/03-01-SUMMARY.md`
- `.archive/planning/phases/03-welcome-tour-and-quick-setup/03-02-PLAN.md`
- `.archive/planning/phases/03-welcome-tour-and-quick-setup/03-02-SUMMARY.md`
- `.archive/planning/phases/03-welcome-tour-and-quick-setup/03-RESEARCH.md`
- `.archive/planning/phases/03-welcome-tour-and-quick-setup/03-VERIFICATION.md`

### .archive/planning/research/
- `.archive/planning/research/ARCHITECTURE.md`
- `.archive/planning/research/DESIGN-REVIEW.html`
- `.archive/planning/research/DESIGN-REVIEW.md`
- `.archive/planning/research/DOORMAN-MILESTONES.md`
- `.archive/planning/research/FEATURES.md`
- `.archive/planning/research/PITFALLS.md`
- `.archive/planning/research/STACK.md`
- `.archive/planning/research/SUMMARY.md`

### .maestro/
- `.maestro/config.yaml`

### .maestro/flows/
- `.maestro/flows/01-happy-path-onboarding.yaml`
- `.maestro/flows/02-skip-everything.yaml`
- `.maestro/flows/03-skip-from-tour.yaml`
- `.maestro/flows/04-skip-quick-setup.yaml`
- `.maestro/flows/05-returning-user.yaml`
- `.maestro/flows/06-revisit-tour.yaml`
- `.maestro/flows/07-bottom-nav.yaml`

### .maestro/helpers/
- `.maestro/helpers/assert-on-main-app.yaml`
- `.maestro/helpers/complete-tour.yaml`
- `.maestro/helpers/skip-all-permissions.yaml`

### app/
- `app/build.gradle.kts`
- `app/proguard-rules.pro`

### app/src/androidTest/java/com/usagecontrol/
- `app/src/androidTest/java/com/usagecontrol/NotificationActionsTest.kt`
- `app/src/androidTest/java/com/usagecontrol/SettingsBackTest.kt`

### app/src/androidTest/java/com/usagecontrol/db/
- `app/src/androidTest/java/com/usagecontrol/db/MigrationTest.kt`
- `app/src/androidTest/java/com/usagecontrol/db/NotificationDaoTest.kt`

### app/src/main/
- `app/src/main/AndroidManifest.xml`

### app/src/main/java/com/usagecontrol/
- `app/src/main/java/com/usagecontrol/BootReceiver.kt`
- `app/src/main/java/com/usagecontrol/MainActivity.kt`
- `app/src/main/java/com/usagecontrol/MainViewModel.kt`
- `app/src/main/java/com/usagecontrol/UsageControlApp.kt`

### app/src/main/java/com/usagecontrol/di/
- `app/src/main/java/com/usagecontrol/di/DataStoreModule.kt`

### app/src/main/java/com/usagecontrol/navigation/
- `app/src/main/java/com/usagecontrol/navigation/AppNavigation.kt`

### app/src/main/res/drawable/
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`

### app/src/main/res/mipmap-anydpi-v26/
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

### app/src/main/res/values/
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`

### app/src/main/res/values-night/
- `app/src/main/res/values-night/colors.xml`
- `app/src/main/res/values-night/themes.xml`

### core/core-common/
- `core/core-common/build.gradle.kts`

### core/core-common/src/main/java/com/usagecontrol/core/common/
- `core/core-common/src/main/java/com/usagecontrol/core/common/AppNameResolver.kt`
- `core/core-common/src/main/java/com/usagecontrol/core/common/NotificationActions.kt`
- `core/core-common/src/main/java/com/usagecontrol/core/common/PermissionChecker.kt`
- `core/core-common/src/main/java/com/usagecontrol/core/common/SoundProfileApplier.kt`
- `core/core-common/src/main/java/com/usagecontrol/core/common/TimeUtils.kt`

### core/core-common/src/main/java/com/usagecontrol/core/common/onboarding/
- `core/core-common/src/main/java/com/usagecontrol/core/common/onboarding/OnboardingDataStore.kt`
- `core/core-common/src/main/java/com/usagecontrol/core/common/onboarding/OnboardingRepository.kt`

### core/core-database/
- `core/core-database/build.gradle.kts`

### core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/
- `core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/3.json`
- `core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/4.json`

### core/core-database/src/main/java/com/usagecontrol/core/database/
- `core/core-database/src/main/java/com/usagecontrol/core/database/DatabaseModule.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/Mappers.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/UsageControlDatabase.kt`

### core/core-database/src/main/java/com/usagecontrol/core/database/dao/
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/ContextSnapshotDao.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/LiveNotificationDao.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/RuleDao.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/SoundPresetDao.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/SuggestionDao.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/UsageDao.kt`

### core/core-database/src/main/java/com/usagecontrol/core/database/entity/
- `core/core-database/src/main/java/com/usagecontrol/core/database/entity/ContextSnapshotEntity.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/entity/LiveNotificationEntity.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/entity/NotificationEntity.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/entity/RuleEntity.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/entity/SoundPresetEntity.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/entity/SuggestionEntity.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/entity/UsageEntity.kt`

### core/core-model/
- `core/core-model/build.gradle.kts`

### core/core-model/src/main/java/com/usagecontrol/core/model/
- `core/core-model/src/main/java/com/usagecontrol/core/model/AppDismissStats.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/ConditionGroup.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/ContextSnapshot.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/NotificationRecord.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/ProvisionalLane.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/Rule.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/RuleAction.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/RuleCondition.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/SoundPreset.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/Suggestion.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/UsageRecord.kt`

### core/core-rules/
- `core/core-rules/build.gradle.kts`

### core/core-rules/src/main/java/com/usagecontrol/core/rules/
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/ActionExecutor.kt`
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/ConditionEvaluator.kt`
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/ProvisionalLaneClassifier.kt`
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/RuleEngine.kt`
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/SuggestionEngine.kt`

### core/core-rules/src/test/java/com/usagecontrol/core/rules/
- `core/core-rules/src/test/java/com/usagecontrol/core/rules/ProvisionalLaneClassifierTest.kt`

### core/core-ui/
- `core/core-ui/build.gradle.kts`

### core/core-ui/licenses/
- `core/core-ui/licenses/DMMono-OFL.txt`
- `core/core-ui/licenses/Manrope-OFL.txt`

### core/core-ui/src/main/java/com/usagecontrol/core/ui/component/
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/Charts.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/CommonComponents.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/ComponentGallery.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/DoormanComponents.kt`

### core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Color.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/LaneColors.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Shape.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Theme.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Type.kt`

### core/core-ui/src/test/java/com/usagecontrol/core/ui/component/
- `core/core-ui/src/test/java/com/usagecontrol/core/ui/component/BundleCountLabelTest.kt`

### core/core-ui/src/test/java/com/usagecontrol/core/ui/theme/
- `core/core-ui/src/test/java/com/usagecontrol/core/ui/theme/LaneColorsTest.kt`

### docs/
- `docs/.nojekyll`
- `docs/index.html`

### feature/feature-learning/
- `feature/feature-learning/build.gradle.kts`

### feature/feature-learning/src/main/java/com/usagecontrol/feature/learning/
- `feature/feature-learning/src/main/java/com/usagecontrol/feature/learning/LearningScreen.kt`
- `feature/feature-learning/src/main/java/com/usagecontrol/feature/learning/LearningViewModel.kt`

### feature/feature-lockcontrol/
- `feature/feature-lockcontrol/build.gradle.kts`

### feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/LockControlScreen.kt`
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/LockControlViewModel.kt`

### feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesLogic.kt`
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesScreen.kt`
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesViewModel.kt`

### feature/feature-lockcontrol/src/test/java/com/usagecontrol/feature/lockcontrol/modes/
- `feature/feature-lockcontrol/src/test/java/com/usagecontrol/feature/lockcontrol/modes/ModesLogicTest.kt`

### feature/feature-notifications/
- `feature/feature-notifications/build.gradle.kts`

### feature/feature-notifications/src/androidTest/java/com/usagecontrol/feature/notifications/
- `feature/feature-notifications/src/androidTest/java/com/usagecontrol/feature/notifications/SwipeArchiveUndoTest.kt`

### feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationIcon.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowScreen.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowStream.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowViewModel.kt`

### feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppProfileScreen.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppProfileViewModel.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsLogic.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsScreen.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppStats.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsViewModel.kt`

### feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/NotificationIconPathTest.kt`
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/NowStreamTest.kt`

### feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/apps/
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/apps/AppsLogicTest.kt`
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/apps/AppStatsTest.kt`

### feature/feature-onboarding/
- `feature/feature-onboarding/build.gradle.kts`

### feature/feature-onboarding/src/main/
- `feature/feature-onboarding/src/main/AndroidManifest.xml`

### feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/FeatureTourScreen.kt`
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionStep.kt`
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughScreen.kt`
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughViewModel.kt`
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/QuickSetupScreen.kt`
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/QuickSetupViewModel.kt`
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/WelcomeScreen.kt`

### feature/feature-onboarding/src/main/res/values/
- `feature/feature-onboarding/src/main/res/values/strings.xml`

### feature/feature-rules/
- `feature/feature-rules/build.gradle.kts`

### feature/feature-rules/src/main/
- `feature/feature-rules/src/main/AndroidManifest.xml`

### feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/
- `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/AddActionDialog.kt`
- `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/AddConditionDialog.kt`
- `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/RuleDisplayHelpers.kt`
- `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/RuleEditorScreen.kt`
- `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/RuleEditorViewModel.kt`
- `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/RuleListScreen.kt`
- `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/RuleListViewModel.kt`

### feature/feature-screentime/
- `feature/feature-screentime/build.gradle.kts`

### feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/
- `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/BlockOverlayActivity.kt`
- `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/ScreenTimeScreen.kt`
- `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/ScreenTimeViewModel.kt`

### feature/feature-settings/
- `feature/feature-settings/build.gradle.kts`

### feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/
- `feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/CleanupWorker.kt`
- `feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt`
- `feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsViewModel.kt`

### feature/feature-settings/src/main/res/values/
- `feature/feature-settings/src/main/res/values/strings.xml`

### gradle/
- `gradle/libs.versions.toml`

### gradle/wrapper/
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

### service/service-context/
- `service/service-context/build.gradle.kts`

### service/service-context/src/main/java/com/usagecontrol/service/context/
- `service/service-context/src/main/java/com/usagecontrol/service/context/ActionHandlersModule.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/BluetoothScanner.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/CalendarReader.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/DeviceAdminReceiver.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/LockControlExecutor.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/SoundPresetApplier.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/SoundProfileApplierImpl.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/SoundProfileExecutor.kt`

### service/service-context/src/main/res/values/
- `service/service-context/src/main/res/values/strings.xml`

### service/service-context/src/main/res/xml/
- `service/service-context/src/main/res/xml/device_admin.xml`

### service/service-notification/
- `service/service-notification/build.gradle.kts`

### service/service-notification/src/main/java/com/usagecontrol/service/notification/
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/CaptureMapping.kt`
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/IconStore.kt`
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationActionsImpl.kt`
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt`
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/PendingIntentCache.kt`

### service/service-notification/src/test/java/com/usagecontrol/service/notification/
- `service/service-notification/src/test/java/com/usagecontrol/service/notification/CaptureMappingTest.kt`
- `service/service-notification/src/test/java/com/usagecontrol/service/notification/IconStorePathTest.kt`

### service/service-usage/
- `service/service-usage/build.gradle.kts`

### service/service-usage/src/main/java/com/usagecontrol/service/usage/
- `service/service-usage/src/main/java/com/usagecontrol/service/usage/UsageMonitorService.kt`

### service/service-usage/src/main/res/values/
- `service/service-usage/src/main/res/values/strings.xml`
