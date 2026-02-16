import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("com.gradleup.shadow")
    id("io.papermc.paperweight.userdev")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
}

// Mojang-mapped output: shadowJar is main artifact (1.20.5+ Paper uses Mojang at runtime)

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from("src/main/resources") {
        filter(mapOf("tokens" to mapOf("version" to version)), ReplaceTokens::class.java)
    }
}

val processJava = tasks.register<Copy>("processJava") {
    from("src/main/java")
    into(layout.buildDirectory.dir("generated-src"))
    filter(mapOf("tokens" to mapOf("version" to version)), ReplaceTokens::class.java)
}

tasks.compileJava {
    dependsOn(processJava)
    setSource(processJava.get().outputs.files)
}

tasks.shadowJar {
    archiveClassifier.set("")
    destinationDirectory.set(project.layout.projectDirectory.dir("target"))
    archiveFileName.set("${project.name}-${project.version}-shaded.jar")
}

tasks.register<Copy>("copyToOutput") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(project.rootProject.layout.projectDirectory.dir("output"))
    rename { "always-online-latest.jar" }
}

tasks.build {
    finalizedBy(tasks.named("copyToOutput"))
}
