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
        versionCode = 1
        versionName = "1.0.0"
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

    buildTypes {
        release {
            isMinifyEnabled = false
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
