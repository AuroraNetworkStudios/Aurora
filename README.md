# Aurora

This is the base plugin for every Aurora plugin.
It requires **PaperMC** as the server software and **Java 17** or higher.
In theory folia is also supported, but it is not tested yet.

You can view the full documentation [here](https://docs.auroramc.gg/aurora).

It provides the following utilities:
- chat/actionbar messaging, text building using every possible color formats and styles.
- custom logger
- automatic yaml to class mapping and config versioning
- plugin dependency management
- inventory menu/gui builder with premade configs
- user data management via file or mysql (mysql can sync between servers as well)
- builtin user metadata store with placeholder support
- economy expansion with builtin providers (CMI, EssentialsX, Vault)
- placeholder expansion to interact with PAPI
- configurable number formatting
- player placed block tracker (supporting flatfile or sqlite as storage) 
which integrates with Multiverse-Core and WildRegeneration for cleanup. It can even handle manual chunk deletions.
- WorldGuard expansion to provide events like `PlayerRegionEnterEvent` and `PlayerRegionLeaveEvent`
- `CommandDispatcher` to easily dispatch commands from config files

## Include it in your project

### Maven

```xml
<repository>
    <id>auroramc</id>
    <url>https://repo.auroramc.gg/repository/maven-public/</url>
</repository>
```

```xml
<dependency>
    <groupId>gg.auroramc</groupId>
    <artifactId>Aurora</artifactId>
    <version>1.2.0</version>
    <scope>provided</scope>
</dependency>
```
### Gradle

**Groovy DSL:**
```gradle
repositories {
    maven {
        url "https://repo.auroramc.gg/repository/maven-public/"
    }
}

dependencies {
    compileOnly 'gg.auroramc:Aurora:1.2.0'
}
```

**Kotlin DSL:**
```Gradle Kotlin DSL
repositories { 
    maven("https://repo.auroramc.gg/repository/maven-public/")
}

dependencies { 
    compileOnly("gg.auroramc:Aurora:1.2.0")
}
```