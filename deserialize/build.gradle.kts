plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    id("com.boswelja.publish")
}

kotlin {
    explicitApi()
    jvmToolchain(21)

    jvm()
    androidLibrary {
        namespace = "com.boswelja.xmldtd.deserialize"
        compileSdk = 36
        minSdk = 23
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt.yml")
    basePath = rootDir.absolutePath
}

publish {
    description = "Deserialize XML DTD into type-safe Kotlin objects."
    repositoryUrl = "https://github.com/kmpdict/xmldtd-kt"
    license = "CC-BY-SA-4.0"
}
