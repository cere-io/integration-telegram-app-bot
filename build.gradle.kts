import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.allopen") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://jitpack.io") }
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.grpc" && requested.name.startsWith("grpc-")) {
            useVersion("1.69.1")
        }
    }
}

dependencies {
    // BOM
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

    // Telegram
    implementation("com.github.omarmiatello.telegram:dataclass-jvm:7.9")

    // Web  
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")
    implementation("io.quarkus:quarkus-smallrye-health")
    
    // gRPC
    implementation("io.quarkus:quarkus-grpc")
    implementation("com.google.protobuf:protobuf-kotlin:4.28.2")
    
    // Multibase for CID decoding
    implementation("com.github.multiformats:java-multibase:v1.1.1")

    // Cache
    implementation("io.quarkus:quarkus-cache")

    // Kotlin
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    // Crypto
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("com.github.cerebellum-network:ddc-encryption-impl-kotlin:1.5.0")
    implementation("org.purejava:tweetnacl-java:1.1.2")
    implementation("org.bitcoinj:bitcoinj-core:0.15.10")

    // Config
    implementation("io.quarkus:quarkus-config-yaml")

    // Build
    implementation("io.quarkus:quarkus-arc")

    // Tests
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

group = "network.cere"

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters.set(true)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}
kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    mustRunAfter("quarkusGenerateCode")
}

tasks.named("quarkusGenerateCode") {
    doFirst {
        println("Generating gRPC code with enhanced Kotlin compatibility...")
    }
}

if (tasks.findByName("generateProto") != null) {
    tasks.named("generateProto") {
        doFirst {
            println("Using Java-compatible protobuf generation for better stability...")
        }
    }
}
