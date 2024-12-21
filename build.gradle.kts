import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport

plugins {
    kotlin("jvm") version "2.1.0"
    
    // For Minestom
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "flooferland.fantoma"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.hypera.dev/snapshots/") // simple-voice-chat-minestom
}

dependencies {
    // Kotlin standard library
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // SLF4J API
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    
    // Testing
    testImplementation(kotlin("test"))
    
    // Minestom
    // (TODO: Switch to net.minestom:server once its finally available)
    // https://mvnrepository.com/artifact/net.minestom/minestom-snapshots
    // https://github.com/emortalmc/NBStom
    // https://github.com/LooFifteen/simple-voice-chat-minestom  /  https://repo.hypera.dev/#/snapshots/dev/lu15/simple-voice-chat-minestom
    implementation("net.minestom:minestom-snapshots:1_21_4-35869c40fb")
    implementation("com.github.emortalmc:NBStom:latest")
    implementation("dev.lu15:simple-voice-chat-minestom:0.1.0-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)  // Minestom has a minimum Java version of 21
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "$group.Main"
        }
    }

    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("") // Prevent the -all suffix on the shadow jar file.
    }
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}