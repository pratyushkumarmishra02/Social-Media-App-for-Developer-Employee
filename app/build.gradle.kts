plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.app.folionet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.folionet"
        minSdk = 24
        targetSdk = 35
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
    buildFeatures{
        viewBinding=true
    }
}



dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation ("com.google.firebase:firebase-storage:21.0.2")


    implementation ("com.facebook.android:facebook-android-sdk:15.2.0")



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.animation.core.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.viewpager2)
    implementation(libs.glide.v4120)
    implementation(libs.chip.navigation.bar)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.github.glide)
    implementation ("com.google.android.gms:play-services-auth:21.3.0")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation ("com.tbuonomo:dotsindicator:4.3")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation (libs.material)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation ("androidx.exifinterface:exifinterface:1.4.1")
    implementation( "androidx.gridlayout:gridlayout:1.1.0")
    implementation ("com.github.yalantis:ucrop:2.2.8-native")
    implementation ("androidx.core:core:1.12.0")
    implementation ("androidx.media3:media3-exoplayer:1.7.1")
    implementation ("androidx.media3:media3-ui:1.7.1")
    //implementation( libs.ffmpeg.kit.full)




    // kapt("com.github.bumptech.glide:compiler:4.12.0")

}