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

val applicationPackage = setting("attrack.packageName", "ATTRACK_PACKAGE_NAME")
val attrackAppId = setting("attrack.appId", "ATTRACK_APP_ID")
val attrackClientKey = setting("attrack.clientKey", "ATTRACK_CLIENT_KEY")
val linkScheme = setting("attrack.linkScheme", default = "https")
val linkHost = setting("attrack.linkHost", default = "api.attrack.co.kr")
val sampleVersionCode = setting("attrack.sample.versionCode", default = "1").toInt()

android {
    namespace = "kr.co.attrack.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = applicationPackage.ifBlank { "kr.co.attrack.template.unconfigured" }
        minSdk = 23
        targetSdk = 36
        versionCode = sampleVersionCode
        versionName = "1.0.$sampleVersionCode"

        buildConfigField("String", "ATTRACK_APP_ID", "\"$attrackAppId\"")
        buildConfigField("String", "ATTRACK_CLIENT_KEY", "\"${attrackClientKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
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
    // Maven resolves Install Referrer, the Advertising ID library and WorkManager transitively.
    implementation(libs.attrack.tracker)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

// Reading/Syncing the template is allowed; building requires your own registered package.
val validateAppPackage = tasks.register("validateAppPackage") {
    doLast {
        require(applicationPackage.matches(Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")) &&
            applicationPackage !in setOf("kr.co.attrack.sample", "kr.co.attrack.template.unconfigured", "com.example.app")) {
            "Set attrack.packageName (or ATTRACK_PACKAGE_NAME) to your own registered Android applicationId"
        }
    }
}
tasks.named("preBuild") { dependsOn(validateAppPackage) }
