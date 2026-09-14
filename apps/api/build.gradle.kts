plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
}

group = "dev.sceneproof"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    from(rootProject.file("demo/between the lines - demo.mp4")) {
        into("demo")
        rename { "source-film.mp4" }
    }
    from(rootProject.file("demo/runtime")) {
        into("demo")
    }
    from(rootProject.file("packages/api-client/openapi.json")) {
        into("static")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<JavaExec>("browserTestServer") {
    dependsOn(tasks.testClasses)
    workingDir = rootProject.projectDir
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "dev.sceneproofbrowser.FindingsBrowserApplicationKt"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
}
