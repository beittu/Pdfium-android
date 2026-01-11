plugins {
    id("com.android.library") version "8.9.1"
    `maven-publish`
}

val tools = mapOf(
    "minSdk" to 21,
    "targetSdk" to 36,
    "compileSdk" to 36,
    "versionCode" to 5,
    "versionName" to "1.9.7"
)

group = "com.github.mystic"

android {
    namespace = "com.shockwave.pdfium"
    compileSdk = tools["compileSdk"] as Int
    
    defaultConfig {
        minSdk = tools["minSdk"] as Int
        targetSdk = tools["targetSdk"] as Int
        versionCode = tools["versionCode"] as Int
        versionName = tools["versionName"] as String
        
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }
    
    buildFeatures {
        buildConfig = true
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
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
    implementation("androidx.appcompat:appcompat:1.7.0")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.realmystic"
            artifactId = "PdfiumAndroid"
            version = "1.9.7"
            
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}