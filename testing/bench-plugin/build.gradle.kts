// The test helper plugin, built against the Aurora jar under test: testing/run.sh passes -PauroraJar=<absolute path>
plugins { id("java") }
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.134-stable")
    compileOnly(files(providers.gradleProperty("auroraJar").get()))
}
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
tasks.jar { archiveFileName.set("AuroraBench.jar") }
