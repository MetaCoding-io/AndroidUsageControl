plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.usagecontrol"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.usagecontrol"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        resValue("string", "app_developer", "Metacoding Solutions LLC")
        resValue("string", "app_copyright", "\u00a9 2026 Metacoding Solutions LLC")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        resValues = true
    }

    sourceSets["androidTest"].assets.srcDir(project(":core:core-database").file("schemas"))

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

hilt { enableAggregatingTask = true }

dependencies {
    // Core modules
    implementation(project(":core:core-common"))
    implementation(project(":core:core-model"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-rules"))
    implementation(project(":core:core-ui"))

    // Feature modules
    implementation(project(":feature:feature-notifications"))
    implementation(project(":feature:feature-learning"))
    implementation(project(":feature:feature-screentime"))
    implementation(project(":feature:feature-lockcontrol"))
    implementation(project(":feature:feature-rules"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-onboarding"))

    // Service modules
    implementation(project(":service:service-notification"))
    implementation(project(":service:service-usage"))
    implementation(project(":service:service-context"))

    // DataStore
    implementation(libs.datastore.preferences)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.navigation)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // kotlinx-serialization alignment (room-testing needs >= 1.8.1 at runtime)
    implementation(platform(libs.kotlinx.serialization.bom))

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.room.runtime)
    androidTestImplementation(libs.coroutines.core)
    androidTestImplementation(libs.datastore.preferences)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
