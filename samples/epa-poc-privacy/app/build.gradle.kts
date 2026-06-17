plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    implementation(libs.hapi.base)
    implementation(libs.hapi.structures.r4)
    implementation(libs.hapi.validation)
    implementation(libs.hapi.validation.resources.r4)
    runtimeOnly(libs.hapi.caching.caffeine)

    implementation(libs.logback.classic)
    implementation(libs.jackson.yaml)
    implementation(libs.jte)
    implementation(libs.jte.kotlin)
    implementation(libs.yamlkt)
    implementation(libs.java.diff.utils)

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "de.gematik.epa.poc.privacy.MainKt"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
