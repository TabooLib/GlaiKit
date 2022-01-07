plugins {
    java
    `maven-publish`
    id("io.izzel.taboolib") version "1.34"
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
}

taboolib {
    install("common")
    install("common-5")
    install(
        "module-configuration",
        "module-chat",
        "module-lang",
        "module-kether",
        "module-effect",
        "module-database",
        "module-nms", // bukkit
        "module-nms-util",
        "module-ui",
        "module-ai",
        "module-porticus", // bungee
    )
    install("expansion-player-database", "expansion-command-helper")
    install(
        "platform-bukkit",
        "platform-bungee"
    )
    options("skip-kotlin-relocate")
    classifier = null
    version = "6.0.7-16"
    exclude("module-info")
}

repositories {
    maven { url = uri("https://repo.tabooproject.org/repository/releases") }
    mavenCentral()
}

dependencies {
    val kotlinVersion = "1.5.10"
    taboo("io.github:fast-classpath-scanner:3.1.13")
    implementation("org.jetbrains.kotlin:kotlin-main-kts:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion")
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v11800:11800:api")
    compileOnly("ink.ptms.core:v11800:11800:mapped")
    compileOnly("ink.ptms.core:v11800:11800:universal")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    repositories {
        maven {
            url = uri("https://repo.tabooproject.org/repository/releases")
            credentials {
                username = project.findProperty("taboolibUsername").toString()
                password = project.findProperty("taboolibPassword").toString()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = "ink.ptms"
        }
    }
}