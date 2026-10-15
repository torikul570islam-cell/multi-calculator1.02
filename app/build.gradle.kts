plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.farazi.multicalculator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.farazi.multicalculator"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        // Google's official test App ID by default. Replace with your own AdMob App ID before release.
        manifestPlaceholders["admobAppId"] = (project.findProperty("ADMOB_APP_ID") as String?) ?: "ca-app-pub-2754574676620387~5821830907"
        // Debug uses Google's test rewarded unit. Release uses the unit ID found in the uploaded HTML.
        buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-2754574676620387/6093293644\"")
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-2754574676620387/8412617069\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("com.google.android.gms:play-services-ads:25.5.0")
}
