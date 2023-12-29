pluginManagement {
   repositories {
      gradlePluginPortal()
      mavenCentral()
      maven("https://maven.neoforged.net/releases/") {
         name = "NeoForged"
      }
      maven("https://repo.spongepowered.org/maven") {
         name = "Sponge Powered"
      }
      maven("https://maven.parchmentmc.org") {
         name = "Parchment MC"
      }
   }
}

rootProject.name = "lazulib"