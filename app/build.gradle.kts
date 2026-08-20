import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}