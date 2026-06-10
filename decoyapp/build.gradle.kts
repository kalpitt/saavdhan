// A HARMLESS test fixture, NOT part of the real product.
//
// It pretends to be "System Update" (a name real malware impersonates) and ships no launcher
// icon and no code at all. Installing it lets us verify that Saavdhan's background watchdog
// correctly flags an impersonating, sideloaded, hidden-icon app and raises a notification.
// It contains ZERO malicious behaviour — it's purely a detection target. See docs/07-testing.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.demo.systemupdate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.demo.systemupdate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
