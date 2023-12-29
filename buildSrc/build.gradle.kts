plugins {
    `kotlin-dsl`
}

repositories {
    // maven("https://files.minecraftforge.net/maven") {
    //     name = "Minecraft Forge"
    // }
    maven("https://repo.spongepowered.org/maven") {
        name = "Sponge Powered"
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // implementation("net.minecraftforge.gradle:ForgeGradle:5.1.+") {
    //     isChanging = true
    // }
    implementation("org.spongepowered:mixingradle:0.7.+")
}