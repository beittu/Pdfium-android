plugins {
    id("com.android.library") version "8.13.2"
    `maven-publish`
}

group = "com.github.mystic"

android {
    namespace = "com.shockwave.pdfium"
    compileSdk { version = release(36) }
    
    defaultConfig {
        minSdk = 23
        // versionCode and versionName are not supported in library modules
        // They are only used in application modules

        buildConfigField("String", "VERSION_NAME", "\"1.9.8\"")

        
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }

    // Configure targetSdk for testing and lint (deprecated in defaultConfig for libraries)
    testOptions {
        targetSdk = 36
    }
    
    lint {
        targetSdk = 36
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    
    ndkVersion = "28.0.13004108"
    
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.realmystic"
            artifactId = "PdfiumAndroid"
            version = "1.9.8"
            
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}