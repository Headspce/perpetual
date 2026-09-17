plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.progranimator.perpetual"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.progranimator.perpetual"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"
        // Google Drive file ID of cartridge.json in the shared "Wren run/perpetual"
        // folder. The file is updated in place, so this ID is stable.
        buildConfigField(
            "String", "MANIFEST_URL",
            "\"https://drive.google.com/uc?export=download&id=14bUYUGzkbVAB6ljlRL28lTbQo0rfZWJ4\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("/tmp/perpetual.p12")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
