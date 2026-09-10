plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tvquiz"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tvquiz"
        // minSdk 21 katab kogu Android TV / Google TV seadmepargi
        minSdk = 21
        targetSdk = 34
        // CI annab ehitusnumbri (GitHub Actions run_number) läbi -PappVersionCode.
        // See läheb otse Android versionCode'iks, nii saab rakendus ise ära tunda,
        // kas serveris on uuem ehitus kui paigaldatud oma.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionCode") as String?) ?: "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        getByName("debug") {
            // Püsiv, repos hoitav debug-võti (app/debug.keystore).
            // Kriitiline: kui iga CI ehitus genereeriks oma võtme, ei saaks Android
            // kunagi üle installida — "pakett on olemasoleva paketiga vastuolus".
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
