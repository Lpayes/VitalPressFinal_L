import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}


// Carga secrets.properties de la raíz (archivo local, no subido)
val secretsFile = rootProject.file("secrets.properties")
val secretsProps = Properties().apply {
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

val openAiKey: String = when {
    project.hasProperty("OPENAI_API_KEY") -> project.property("OPENAI_API_KEY") as String
    secretsProps.getProperty("OPENAI_API_KEY") != null -> secretsProps.getProperty("OPENAI_API_KEY")
    else -> "AQUI_VA_OPENAI_API_KEY"
}

val documentAiKey: String = when {
    project.hasProperty("DOCUMENTAI_API_KEY") -> project.property("DOCUMENTAI_API_KEY") as String
    secretsProps.getProperty("DOCUMENTAI_API_KEY") != null -> secretsProps.getProperty("DOCUMENTAI_API_KEY")
    else -> "AQUI_VA_DOCUMENTAI_API_KEY"
}

android {
    namespace = "com.example.vitalpreesoficial"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.vitalpreesoficial"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exponer las claves al código sin subirlas: BuildConfig.OPENAI_API_KEY / BuildConfig.DOCUMENTAI_API_KEY
        buildConfigField("String", "OPENAI_API_KEY", "\"${openAiKey}\"")
        buildConfigField("String", "DOCUMENTAI_API_KEY", "\"${documentAiKey}\"")
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

    // Evitar conflictos de META-INF durante merge de recursos Java
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Google Sign-In (OAuth) - para obtener token de usuario
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // OkHttp para hacer llamadas REST a Document AI
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // ML Kit: OCR de texto en dispositivo
    implementation("com.google.mlkit:text-recognition:16.0.0")
}