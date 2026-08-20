import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val alchemyKey =
    System.getenv("ALCHEMY_BTC_API_KEY")
        ?: localProperties.getProperty("ALCHEMY_BTC_API_KEY")
        ?: ""

fun String.quotedForBuildConfig(): String {
    val escaped = replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"" + escaped + "\""
}

android {
    namespace = "network.bahn.androidcryptowallet"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "network.bahn.androidcryptowallet"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "ALCHEMY_BTC_API_KEY", alchemyKey.quotedForBuildConfig())
            buildConfigField(
                "String",
                "ALCHEMY_BASE_URL",
                "https://bitcoin-testnet4.g.alchemy.com/v2/".quotedForBuildConfig(),
            )
        }
        release {
            optimization {
                enable = false
            }
            buildConfigField("String", "ALCHEMY_BTC_API_KEY", alchemyKey.quotedForBuildConfig())
            buildConfigField(
                "String",
                "ALCHEMY_BASE_URL",
                "https://bitcoin-mainnet.g.alchemy.com/v2/".quotedForBuildConfig(),
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
