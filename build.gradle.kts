// Top-level build file. It just declares the plugins the project uses; "apply false" means
// "make them available, but switch them on inside the app module, not here".
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
