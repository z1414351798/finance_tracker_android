plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.z.financetracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.z.financetracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // ── BOTH flags must be in the same buildFeatures block ─────────
    buildFeatures {
        compose = true       // ← was missing, causes compose errors
        buildConfig = true   // ← enables BuildConfig generation
    }

    flavorDimensions += "env"

    productFlavors {
        // Android emulator only — maps 10.0.2.2 → your Mac's localhost
        create("local") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2/\"")
        }
        // Real phone testing — use wifi test
        create("wifi") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"http://10.172.9.160/\"")
        }
        // Real phone testing — ngrok tunnels your Mac's backend over HTTPS
        create("ngrok") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://nontoxically-unhumiliating-shoshana.ngrok-free.dev/\"")
        }
        // Production server — your VPS with nginx + Let's Encrypt
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://yourdomain.com/\"")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}