plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    id("com.boswelja.publish")
}

kotlin {
    explicitApi()
    jvmToolchain(21)
}

dependencies {
    api(projects.deserialize)
    implementation(libs.square.kotlinpoet)
    implementation(libs.kotlinx.serialization.xml)

    testImplementation(libs.kotlin.test)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt.yml")
    basePath = rootDir.absolutePath
}

publish {
    description = "Generate Kotlin code from XML DTD."
    repositoryUrl = "https://github.com/kmpdict/xmldtd-kt"
    license = "CC-BY-SA-4.0"
}
