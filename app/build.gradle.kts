
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
}

// Read Supabase configuration from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

val supabaseUrl =
    localProperties.getProperty("SUPABASE_URL", "")

val supabasePublishableKey =
    localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "")

println("LiFE360 Supabase URL loaded: ${supabaseUrl.startsWith("https://") && supabaseUrl.endsWith(".supabase.co")}")
println("LiFE360 Supabase key loaded: ${supabasePublishableKey.isNotBlank()}")

android {
    namespace = "com.life360.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.life360.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // Make Supabase values available to Kotlin
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"$supabaseUrl\""
        )

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"$supabasePublishableKey\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // GPS location
    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )

    // Supabase integration
    implementation(
        platform("io.github.jan-tennert.supabase:bom:3.5.0")
    )
    implementation(
        "io.github.jan-tennert.supabase:postgrest-kt"
    )
    implementation(
        "io.github.jan-tennert.supabase:auth-kt"
    )
    implementation("io.ktor:ktor-client-android:3.0.3")

    implementation("io.github.jan-tennert.supabase:functions-kt")

    // Testing dependencies
    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Debug dependencies
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
