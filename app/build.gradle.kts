import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val mockWalletTestnet4 =
    System.getenv("MOCK_BITCOIN_WALLET_TESTNET4")
        ?: localProperties.getProperty("MOCK_BITCOIN_WALLET_TESTNET4")
        ?: ""

val mockWalletMainnet =
    System.getenv("MOCK_BITCOIN_WALLET_MAINNET")
        ?: localProperties.getProperty("MOCK_BITCOIN_WALLET_MAINNET")
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

        buildConfigField(
            "String",
            "MOCK_BITCOIN_WALLET_TESTNET4",
            mockWalletTestnet4.quotedForBuildConfig(),
        )
        buildConfigField(
            "String",
            "MOCK_BITCOIN_WALLET_MAINNET",
            mockWalletMainnet.quotedForBuildConfig(),
        )
        buildConfigField(
            "String",
            "MS_TESTNET4_BASE_URL",
            "https://mempool.space/testnet4/api/".quotedForBuildConfig(),
        )
        buildConfigField(
            "String",
            "MS_MAINNET_BASE_URL",
            "https://mempool.space/api/".quotedForBuildConfig(),
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.bdk.android)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
