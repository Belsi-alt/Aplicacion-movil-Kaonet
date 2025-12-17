// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // AGREGAR Firebase y Google Services
    id("com.google.gms.google-services") version "4.4.4" apply false
}

// Si no tienes libs.versions.toml, usa esta versión:
// id 'com.google.gms.google-services' version '4.4.0' apply false