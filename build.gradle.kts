import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.allopen") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    kotlin("plugin.jpa") version "2.0.0"
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://repo.repsy.io/mvn/chrynan/public") }
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    // BOM
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

    // Telegram
    implementation("com.github.omarmiatello.telegram:dataclass:7.8")

    // DB
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")

    // Cache
    implementation("io.quarkus:quarkus-cache")

    // gRPC
    implementation("io.quarkus:quarkus-grpc")

    // Web
    implementation("io.quarkus:quarkus-rest-kotlin-serialization")
    implementation("io.quarkus:quarkus-rest-client-kotlin-serialization")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-smallrye-health")

    // Kotlin
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Crypto
    implementation("dev.sublab:encrypting-kotlin:1.0.0")
    implementation("dev.sublab:hashing-kotlin:1.0.0")
    implementation("dev.sublab:common-kotlin:1.0.0")

    // Config
    implementation("io.quarkus:quarkus-config-yaml")

    // Build
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-container-image-jib")

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
    }
}
kotlin {
    jvmToolchain(21)
}

java {
}