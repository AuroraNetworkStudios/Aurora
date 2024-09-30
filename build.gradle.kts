import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI
import java.util.*

fun loadProperties(filename: String): Properties {
    val properties = Properties()
    if (!file(filename).exists()) {
        return properties
    }
    file(filename).inputStream().use { properties.load(it) }
    return properties
}

plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
    id("maven-publish")
}

group = "gg.auroramc"
version = "1.6.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    flatDir {
        dirs("libs")
    }
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.helpch.at/releases/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.onarandombox.com/content/groups/public/")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
    maven("https://repo.oraxen.com/snapshots")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    //maven("https://repo.auxilor.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    // 3rd party
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.essentialsx:EssentialsX:2.19.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("com.onarandombox.multiversecore:multiverse-core:4.3.12")
    compileOnly("com.github.angeschossen:WildRegenerationAPI:1.5.0")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("io.lumine:Mythic-Dist:5.6.1")
    compileOnly("com.github.Xiao-MoMi:Custom-Fishing:2.2.26")
    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT")
    compileOnly("io.th0rgal:oraxen:2.0-SNAPSHOT")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    //compileOnly("com.willfp:eco:6.73.0")

    // 3rd party local
    compileOnly(name = "CMI9.0.0.0API", group = "com.Zrips.CMI", version = "9.0.0.0")
    compileOnly(name = "eco-6.73.1-all", group = "com.willfp", version = "6.73.1")
    compileOnly(name = "SCore-5.24.8.25", group = "com.ssomar.score", version = "5.24.8.25")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // HikariCP
    implementation("com.zaxxer:HikariCP:5.1.0") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    // exp4j
    implementation("net.objecthunter:exp4j:0.4.8")

    // ACF
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")

    // JUnit Jupiter API and Engine dependencies
    testImplementation("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("Aurora-${project.version}.jar")

    relocate("com.zaxxer.hikari", "gg.auroramc.aurora.libs.hikari")
    relocate("net.objecthunter.exp4j", "gg.auroramc.aurora.libs.exp4j")

    relocate("co.aikar.commands", "gg.auroramc.quests.libs.acf")
    relocate("co.aikar.locales", "gg.auroramc.quests.libs.locales")

    exclude("acf-*.properties")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

val publishing = loadProperties("publish.properties")

publishing {
    repositories {
        maven {
            name = "AuroraMC"
            url = if (version.toString().endsWith("SNAPSHOT")) {
                URI.create("https://repo.auroramc.gg/repository/maven-snapshots/")
            } else {
                URI.create("https://repo.auroramc.gg/repository/maven-releases/")
            }
            credentials {
                username = publishing.getProperty("username")
                password = publishing.getProperty("password")
            }
        }
    }

    publications.create<MavenPublication>("mavenJava") {
        groupId = "gg.auroramc"
        artifactId = "Aurora"
        version = project.version.toString()

        from(components["java"])
    }
}


