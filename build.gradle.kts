import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import java.text.SimpleDateFormat
import java.util.*

buildscript {
    repositories {
        maven("https://files.minecraftforge.net/maven")
        mavenCentral()
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.1.+") {
            isChanging = true
        }
    }
}

// Plugins
plugins {
    java
    id("net.minecraftforge.gradle")
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

// Mod info --------------------------------------------------------------------

val modId = "lazulib"
val modGroup = "endorh.util.lazulib"
val githubRepo = "endorh/lazulib"
val modVersion = "1.0.0"
val mcVersion = "1.16.5"
val forge = "36.1.0"
val forgeVersion = "$mcVersion-$forge"
val mappingsChannel = "snapshot"
val mappingsVersion = "20201028-1.16.3"

val groupSlashed = modGroup.replace(".", "/")
val className = "LazuLib"
val modArtifactId = "$modId-$mcVersion"
val modMavenArtifact = "$modGroup:$modArtifactId:$modVersion"

group = modGroup
version = modVersion

// Attributes
val displayName = "LazuLib"
val vendor = "Endor H"
val credits = ""
val authors = "Endor H"
val issueTracker = "https://github.com/$githubRepo/issues"
val page = "https://www.curseforge.com/minecraft/mc-mods/lazulib"
val updateJson = "https://github.com/$githubRepo/raw/updates/updates.json"
val logoFile = "$modId.png"
val modDescription = """
    Modding library used by Endor H mods.
    Feel free to use.
""".trimIndent()

// License
val license = "MIT"

val jarAttributes = mapOf(
    "Specification-Title"      to modId,
    "Specification-Vendor"     to vendor,
    "Specification-Version"    to "1",
    "Implementation-Title"     to name,
    "Implementation-Version"   to version,
    "Implementation-Vendor"    to vendor,
    "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
    "Maven-Artifact"           to modMavenArtifact
)

val modProperties = mapOf(
    "modid"         to modId,
    "display"       to displayName,
    "version"       to version,
    "mcversion"     to mcVersion,
    "vendor"        to vendor,
    "authors"       to authors,
    "credits"       to credits,
    "license"       to license,
    "page"          to page,
    "issue_tracker" to issueTracker,
    "update_json"   to updateJson,
    "logo_file"     to logoFile,
    "description"   to modDescription,
    "group"         to group,
    "class_name"    to className,
    "group_slashed" to groupSlashed
)

// Source Sets -----------------------------------------------------------------

sourceSets.main.get().resources {
    // Include resources generated by data generators.
    srcDir("src/generated/resources")
}

// Java options ----------------------------------------------------------------

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

println(
    "Java: " + System.getProperty("java.version")
    + " JVM: " + System.getProperty("java.vm.version") + "(" + System.getProperty("java.vendor")
    + ") Arch: " + System.getProperty("os.arch"))

// Minecraft options -----------------------------------------------------------

minecraft {
    mappings(mappingsChannel, mappingsVersion)
    
    // Run configurations
    runs {
        val client = create("client") {
            workingDirectory(file("run"))
        
            // Allowed flags: SCAN, REGISTRIES, REGISTRYDUMP
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("mixin.env.disableRefMap", "true")
        
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }
    
        create("server") {
            workingDirectory(file("run"))
        
            // Allowed flags: SCAN, REGISTRIES, REGISTRYDUMP
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("mixin.env.disableRefMap", "true")
        
            arg("nogui")
        
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }
    
        create("client2") {
            parent(client)
            args("--username", "Dev2")
        }
    }
}

// Project dependencies -----------------------------------------------------------

repositories {
    maven("https://maven.shedaniel.me/") {
        name = "Cloth Config API"
    }
    maven("https://repo.maven.apache.org/maven2") {
        name = "Maven Central"
    }
    mavenCentral()
}

dependencies {
    // IDE
    implementation("org.jetbrains:annotations:23.0.0")
    implementation("org.junit.jupiter:junit-jupiter:5.9.0")

    // Minecraft
    minecraft("net.minecraftforge:forge:$forgeVersion")
    
    // Recursive Regex (https://github.com/florianingerl/com.florianingerl.util.regex)
    implementation("com.github.florianingerl.util:regex:1.1.9")

    // Simple regex valid matches generator (https://github.com/curious-odd-man/RgxGen)
    implementation("com.github.curious-odd-man:rgxgen:1.4")
}

// Tasks --------------------------------------------------------------------------

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.classes {
    dependsOn(tasks.extractNatives.get())
}

lateinit var reobfJar: RenameJarInPlace
lateinit var reobfShadowJar: RenameJarInPlace
reobf {
    reobfJar = create("jar")
    reobfShadowJar = create("shadowJar")
}

tasks.shadowJar {
    archiveBaseName.set(modArtifactId)
    archiveClassifier.set("") // Replace default jar
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    dependencies {
        include(dependency("com.github.florianingerl.util:regex:1.1.9"))
        include(dependency("com.github.curious-odd-man:rgxgen:1.4"))
    }
    
    val shadowRoot = "$group.shadowed"
    val relocatedPackages = listOf(
        "com.florianingerl",
        "com.github.curiousoddman",
    )
    relocatedPackages.forEach { relocate(it, "$shadowRoot.$it") }
    
    manifest {
        attributes(jarAttributes)
    }
    
    finalizedBy(reobfShadowJar)
}

val sourcesJarTask = tasks.register<Jar>("sourcesJar") {
    group = "build"
    archiveBaseName.set(modArtifactId)
    archiveClassifier.set("sources")
    
    from(sourceSets.main.get().allJava)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(jarAttributes)
        attributes(mapOf("Maven-Artifact" to "$modMavenArtifact:${archiveClassifier.get()}"))
    }
}

val deobfJarTask = tasks.register<Jar>("deobfJar") {
    group = "build"
    archiveBaseName.set(modArtifactId)
    archiveClassifier.set("deobf")
    
    manifest {
        attributes(jarAttributes)
        attributes(mapOf("Maven-Artifact" to "$modMavenArtifact:${archiveClassifier.get()}"))
    }
}

tasks.jar {
    archiveBaseName.set(modArtifactId)
    archiveClassifier.set("flat")
    
    manifest {
        attributes(jarAttributes)
        attributes(mapOf("Maven-Artifact" to "$modMavenArtifact:${archiveClassifier.get()}"))
    }
    
    finalizedBy(reobfJar)
}

// Process resources
tasks.processResources {
    inputs.properties(modProperties)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    // Exclude development files
    exclude("**/.dev/**")
    
    from(sourceSets.main.get().resources.srcDirs) {
        // Expand properties in manifest files
        filesMatching(listOf("**/*.toml", "**/*.mcmeta")) {
            expand(modProperties)
        }
        // Expand properties in JSON resources except for translations
        filesMatching("**/*.json") {
            if (!path.contains("/lang/"))
                expand(modProperties)
        }
    }
}

// Publishing ------------------------------------------------------------------

artifacts {
    archives(tasks.shadowJar.get())
    archives(sourcesJarTask)
    archives(tasks.jar.get())
    archives(deobfJarTask)
}

publishing {
    repositories {
        maven("https://maven.pkg.github.com/$githubRepo") {
            name = "GitHubPackages"
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
        
        maven(rootProject.projectDir.parentFile.resolve("maven")) {
            name = "LocalMods"
        }
    }
    
    publications {
        register<MavenPublication>("mod") {
            artifactId = modArtifactId
            version = modVersion
        
            artifact(tasks.shadowJar.get())
            artifact(sourcesJarTask)
            artifact(deobfJarTask)
        
            pom {
                name.set(displayName)
                url.set(page)
                description.set(modDescription)
            }
        }
    }
}
