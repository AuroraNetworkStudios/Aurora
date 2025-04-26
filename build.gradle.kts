import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.util.Node
import groovy.util.NodeList
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
    id("com.gradleup.shadow") version "8.3.3"
    id("maven-publish")
}

group = "gg.auroramc"
version = "2.3.1"

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
    // PaperMC
    maven("https://repo.papermc.io/repository/maven-public/")
    // PlaceholderAPI
    maven("https://repo.helpch.at/releases/")
    // WorldGuard, WorldEdit
    maven("https://maven.enginehub.org/repo/")
    // EssentialsX
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.essentialsx.net/snapshots/")
    // MultiVerse-Core
    maven("https://repo.onarandombox.com/content/groups/public/")
    // MMO plugins
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
    // Oraxen
    maven("https://repo.oraxen.com/snapshots")
    // Mythic plugins
    maven("https://mvn.lumine.io/repository/maven-public/")
    // ACF
    maven("https://repo.aikar.co/content/groups/aikar/")
    // ELiteMobs, MagmaCore
    maven("https://repo.magmaguy.com/releases")
    // PlayerPoints
    maven("https://repo.rosewooddev.io/repository/public/")
    // Eco plugins
    maven("https://repo.auxilor.io/repository/maven-public/")
    // Nexo
    maven("https://repo.nexomc.com/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // 3rd party
    compileOnly("net.essentialsx:EssentialsX:2.21.0-SNAPSHOT") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("io.lumine:Mythic-Dist:5.6.1")
    compileOnly("com.github.Xiao-MoMi:Custom-Fishing:2.3.3")
    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT")
    compileOnly("io.th0rgal:oraxen:2.0-SNAPSHOT")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.2")
    compileOnly("com.magmaguy:EliteMobs:9.1.11")
    compileOnly("com.magmaguy:MagmaCore:1.4")
    compileOnly("org.black_ixx:playerpoints:3.2.7")
    compileOnly("com.willfp:eco:6.74.2")
    compileOnly("com.willfp:EcoBits:1.8.4")
    compileOnly("com.nexomc:nexo:0.1.0-dev.0")

    // 3rd party local
    compileOnly(name = "CMI9.0.0.0API", group = "com.Zrips.CMI", version = "9.0.0.0")
    //compileOnly(name = "eco-6.73.1-all", group = "com.willfp", version = "6.73.1")
    compileOnly(name = "SCore-5.24.11.13", group = "com.ssomar.score", version = "5.24.11.13")
    compileOnly(name = "ExecutableBlocks-5.24.11.13", group = "com.ssomar.score", version = "5.24.11.13")
    compileOnly(name = "CoinsEngine-2.3.5", group = "su.nightexpress.coinsengine", version = "2.3.5")
    compileOnly(name = "RoyaleEconomyAPI", group = "me.qKing12", version = "1.0")

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

    // bSats
    implementation("org.bstats:bstats-bukkit:3.0.2")

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

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }

    relocate("com.zaxxer.hikari", "gg.auroramc.aurora.libs.hikari")
    relocate("net.objecthunter.exp4j", "gg.auroramc.aurora.libs.exp4j")

    relocate("co.aikar.commands", "gg.auroramc.aurora.libs.acf")
    relocate("co.aikar.locales", "gg.auroramc.aurora.libs.locales")

    relocate("org.bstats", "gg.auroramc.aurora.libs.bstats")

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
                URI.create("https://repo.auroramc.gg/snapshots/")
            } else {
                URI.create("https://repo.auroramc.gg/releases/")
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

        pom.withXml {
            val dependency = (asNode().get("dependencies") as NodeList).first() as Node
            (dependency.get("dependency") as NodeList).forEach {
                val node = it as Node
                val artifactIdList = node.get("artifactId") as NodeList
                val artifactId = (artifactIdList.first() as Node).text()
                if (artifactId in listOf("acf-paper")) {
                    assert(it.parent().remove(it))
                }
            }
        }
    }
}


