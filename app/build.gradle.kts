plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.ohav.currency"
    minSdk = 24
    targetSdk = 36
    // CI passes these for every published release; local builds use the defaults.
    versionCode = providers.gradleProperty("appVersionCode").orNull?.toInt() ?: 5
    versionName = providers.gradleProperty("appVersionName").orNull ?: "5.0.0-dev"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    // Google Play upload key (CI decodes it from a secret, see release.yml). Never in git; without it the Play
    // build is signed with the shared key below, which Play rejects - fine for testing.
    val uploadKeystore = file(System.getenv("UPLOAD_KEYSTORE") ?: "${rootDir}/upload-key.jks")
    if (uploadKeystore.exists()) {
      create("upload") {
        storeFile = uploadKeystore
        storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
      }
    }
    // Shared debug key (committed on purpose) so APKs from any machine or CI install over each other.
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Signed per store: see productFlavors.
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  // Where the build goes. "github": the APK on GitHub Releases, with the in-app updater, everything
  // unlocked and no ads (the owner's own build). "play": Google Play - no self-update, ads and premium.
  flavorDimensions += "store"
  productFlavors {
    create("github") {
      dimension = "store"
      isDefault = true
      buildConfigField("boolean", "SELF_UPDATE", "true")
      // Every GitHub release uses the shared key, so the in-app updater can install them over each other.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
    create("play") {
      dimension = "store"
      buildConfigField("boolean", "SELF_UPDATE", "false")
      signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debugConfig")
      // AdMob ids live in gradle.properties. -PadmobTestAds=true (CI's emulator and branch builds) keeps Google's test
      // ids: real ads shown on an emulator or tapped while testing count as invalid traffic.
      val testAds = providers.gradleProperty("admobTestAds").orNull == "true"
      fun admob(name: String, test: String) =
        if (testAds) test else providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() } ?: test
      manifestPlaceholders["admobAppId"] = admob("admobAppId", "ca-app-pub-3940256099942544~3347511713")
      buildConfigField("String", "AD_UNIT_BANNER", "\"${admob("admobBanner", "ca-app-pub-3940256099942544/9214589741")}\"")
      buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"${admob("admobInterstitial", "ca-app-pub-3940256099942544/1033173712")}\"")
      buildConfigField("String", "AD_UNIT_REWARDED", "\"${admob("admobRewarded", "ca-app-pub-3940256099942544/5224354917")}\"")
    }
  }

  // The app switches language by itself, so every language must reach every device (no language splits).
  bundle { language { enableSplit = false } }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// The Play release goes through R8 (a much smaller download); the GitHub build stays as the owner runs it.
androidComponents {
  beforeVariants(selector().withFlavor("store" to "play").withBuildType("release")) { variant ->
    variant.isMinifyEnabled = true
    variant.shrinkResources = true
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  "playImplementation"(libs.play.billing)
  "playImplementation"(libs.play.services.ads)
  "playImplementation"(libs.play.review)
  "playImplementation"(libs.play.app.update)
  "playImplementation"(libs.ump)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
