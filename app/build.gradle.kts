import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Mechanical code-style enforcement (CI runs ktlintCheck; fix locally with ktlintFormat).
    alias(libs.plugins.ktlint)
}

// Release signing: reads keystore.properties at the repo root IF it exists (see
// keystore.properties.example). The file and the keystore itself are gitignored — never commit
// them. Without the file, release builds stay unsigned (CI builds them that way on purpose).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Beta signing: CI decodes a dedicated BETA keystore (a GitHub secret, never the release key) and
// points these env vars at it, so every beta build is signed identically and installs over the
// previous beta. Without them (e.g. a local build), beta falls back to the debug key.
val betaStoreFile: String? = System.getenv("SAAVDHAN_BETA_STORE_FILE")
val betaPassword: String? = System.getenv("SAAVDHAN_BETA_PASSWORD")
val hasBetaKey = !betaStoreFile.isNullOrBlank() && !betaPassword.isNullOrBlank()

android {
    // The internal package id used for resources. Matches our code package.
    namespace = "com.saavdhan.app"
    // We compile against the Android 35 toolkit...
    compileSdk = 35

    defaultConfig {
        applicationId = "com.saavdhan.app"
        // ...but the app still runs on phones as old as Android 7 (API 24).
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "0.6.0"

        // Ship ONLY English and Hindi resources.
        resourceConfigurations += listOf("en", "hi")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
        if (hasBetaKey) {
            create("beta") {
                storeFile = file(betaStoreFile!!)
                storePassword = betaPassword
                keyAlias = "saavdhan-beta"
                keyPassword = betaPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // Tester builds from CI (see .github/workflows/beta.yml). Behaves exactly like release,
        // but installs NEXT TO the stable app as a separate "Saavdhan Beta" (own package id), so
        // testing never touches the stable install.
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta." + (System.getenv("GITHUB_RUN_NUMBER") ?: "local")
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName(if (hasBetaKey) "beta" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        // Turn on Jetpack Compose (the modern way to build Android screens).
        compose = true
        // Generates BuildConfig, which lets code ask "is this a debug build?" (BuildConfig.DEBUG).
        buildConfig = true
    }

    // Keep the release APK filename stable across versions so the landing page direct-download
    // URL (releases/latest/download/saavdhan.apk) never breaks after an update.
    applicationVariants.all {
        outputs.all {
            val out = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (buildType.name == "release") {
                out.outputFileName = "saavdhan.apk"
            }
            if (buildType.name == "beta") {
                out.outputFileName = "saavdhan-beta.apk"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose is delivered as a "BOM" (bill of materials): one version line keeps all the
    // Compose pieces below on matching, compatible versions.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Background work: periodic scan for newly-installed dangerous apps (the watchdog).
    implementation(libs.androidx.work.runtime)

    // Only included in debug builds: live previews of screens inside Android Studio.
    debugImplementation(libs.androidx.ui.tooling)

    // Fast tests that run on your computer (no phone needed) — this is how we test the brain.
    testImplementation(libs.junit)
}
