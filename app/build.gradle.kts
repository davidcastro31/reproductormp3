plugins {
    // Asegúrate de que este alias esté correcto y apunte al plugin de aplicación de Android
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.reproductormp3"
    // Es bueno usar el SDK más reciente para compilar
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.reproductormp3"
        minSdk = 24
        targetSdk = 36 // Asegúrate de que esto coincida con compileSdk o sea inferior
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Configuración para Room (exportación de esquemas)
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
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
        // Java 11 es una buena elección moderna
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        // Habilita ViewBinding para un acceso más seguro a las vistas
        viewBinding = true
    }
}

dependencies {
    // --- Android Core & UI ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // --- Room Database ---
    // Usaremos la versión más reciente (2.6.1) para evitar el error de metadatos de Kotlin anterior
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    // Se recomienda usar Room KTX (aunque uses Java) para el LiveData, simplifica las coroutines si las usas
    // Si libs.room.common.jvm es una dependencia antigua, puedes omitirla o usar esta:
    // implementation("androidx.room:room-common:2.6.1")

    // --- Lifecycle components ---
    // Versiones estables
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    // --- Media & Audio ---
    implementation("androidx.media:media:1.7.0")
    // ExoPlayer (Ideal para reproductores avanzados)
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")

    // --- Image Loading (Glide) ---
    // Para cargar carátulas de forma eficiente
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // --- Custom UI ---
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // --- Utilidades ---
    // Gson (para JSON, aunque no lo necesitemos aún para lo local)
    implementation("com.google.code.gson:gson:2.10.1")

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
