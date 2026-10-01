import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Per-app values come from -P, ~/.gradle/gradle.properties, local.properties
// or the environment, so nothing app-specific is committed with the sample.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use(::load)
}
fun setting(name: String, env: String? = null, default: String = ""): String =
    providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: env?.let { System.getenv(it) }
        ?: default

val attrackAppId = setting("attrack.appId", "ATTRACK_APP_ID")
val attrackClientKey = setting("attrack.clientKey", "ATTRACK_CLIENT_KEY")
val linkScheme = setting("attrack.linkScheme", default = "https")
val linkHost = setting("attrack.linkHost", default = "api.attrack.co.kr")
val sampleVersionCode = setting("attrack.sample.versionCode", default = "1").toInt()

android {
    namespace = "kr.co.attrack.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.co.attrack.sample"
        minSdk = 23
        targetSdk = 36
        versionCode = sampleVersionCode
        versionName = "1.0.$sampleVersionCode"

        buildConfigField("String", "ATTRACK_APP_ID", "\"$attrackAppId\"")
        manifestPlaceholders["attrackClientKey"] = attrackClientKey
        manifestPlaceholders["attrackLinkScheme"] = linkScheme
        manifestPlaceholders["attrackLinkHost"] = linkHost
    }

    signingConfigs {
        // Supply your own upload key to build a store-ready release; without
        // one the sample signs release with the debug key so it still installs.
        val storeFile = setting("attrack.release.storeFile")
        if (storeFile.isNotBlank()) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = setting("attrack.release.storePassword")
                keyAlias = setting("attrack.release.keyAlias")
                keyPassword = setting("attrack.release.keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            // R8 on, to prove the SDK's consumer rules survive a host shrink.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // The SDK comes from an AAR in app/libs when one is present; otherwise from
    // the Maven artifact, which also brings Install Referrer and WorkManager.
    val localAar = file("libs/attrack-tracker-${libs.versions.attrack.get()}.aar")
    if (localAar.isFile) {
        implementation(files(localAar))
        implementation(libs.installreferrer)
        implementation(libs.work.runtime)
    } else {
        implementation(libs.attrack.tracker)
    }
    implementation(libs.play.services.ads.identifier)

    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
