plugins {
    id("com.android.application")
}

android {
    namespace = "com.webviewbuilder.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.webviewbuilder.app"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Start.io (StartApp) App ID, from the STARTIO_APP_ID Gradle property
        // (gradle.properties, overridable per build by scripts/configure.py).
        // "0" means Start.io ads are disabled.
        val startioAppId = (findProperty("STARTIO_APP_ID") as String?)
            ?.trim()?.ifEmpty { "0" } ?: "0"
        manifestPlaceholders["STARTIO_APP_ID"] = startioAppId
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // Start.io in-app ads SDK (banner ads). Repositories (mavenCentral) are
    // declared centrally in settings.gradle.kts.
    // https://support.start.io - "Integration via Maven"
    // Pinned to 5.2.0, the newest 5.x compatible with this template:
    // 5.2.6+ require minSdk 23 and 5.3.2+ require compileSdk 36/37, while
    // this template uses minSdk 21 / compileSdk 35 (AGP 8.7.3).
    implementation("com.startapp:inapp-sdk:5.2.0")
}
