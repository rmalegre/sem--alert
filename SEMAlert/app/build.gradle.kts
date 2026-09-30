plugins {
    id("com.android.application")
}

android {
    namespace = "ar.edu.unlp.semalert"
    compileSdk = 37

    defaultConfig {
        applicationId = "ar.edu.unlp.semalert"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
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

}

dependencies {
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("androidx.core:core:1.19.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
