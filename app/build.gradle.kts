plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.kushmakar.akashjewellers"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kushmakar.akashjewellers"
        minSdk = 26
        targetSdk = 36
        versionCode = 29
        versionName = "2.9"
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
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/**"
        }
    }
    packaging {

        dex {
            useLegacyPackaging = false
        }
    }
    dexOptions {
        jumboMode= true
        preDexLibraries= true
    }

    // Enable multidex if you have a large app
    defaultConfig {
        multiDexEnabled= true
    }

}

dependencies {

    implementation(libs.firebase.analytics)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.play.services.maps)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.firestore)
    implementation(libs.preference)
    implementation(libs.firebase.config)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.recaptcha)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.play.services)
    implementation(libs.firebase.messaging)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.canhub.cropper)
    implementation(libs.browser)
    implementation(libs.play.integrity)
    implementation(libs.firebase.core)
    implementation(libs.firebase.auth)
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.auth.library)
    implementation(libs.json.org)

}