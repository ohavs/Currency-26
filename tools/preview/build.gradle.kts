plugins {
  kotlin("jvm") version "2.2.10"
  id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
  application
}


sourceSets {
  main { kotlin.srcDirs("stubs", "render", "app") }
  test { kotlin.srcDirs("apptest") }
}

dependencies {
  implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:${providers.gradleProperty("cv").getOrElse("1.7.0")}")
  implementation("org.jetbrains.compose.material3:material3-desktop:${providers.gradleProperty("cv").getOrElse("1.7.0")}")
  implementation("org.jetbrains.compose.material:material-icons-extended-desktop:${providers.gradleProperty("cv").getOrElse("1.7.0")}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
  compileOnly("org.robolectric:android-all:15-robolectric-12650502")
  testImplementation("junit:junit:4.13.2")
}

kotlin { jvmToolchain(21) }
application { mainClass.set("render.RenderKt") }
tasks.named<JavaExec>("run") { workingDir = projectDir }

// androidx lifecycle/arch jvm artifacts live only on Google Maven (unreachable here); the image renderer doesn't need them.
listOf("runtimeClasspath", "testRuntimeClasspath").forEach { name -> configurations.named(name) {
  exclude(group = "androidx.lifecycle")
  exclude(group = "androidx.arch.core")
  exclude(group = "androidx.annotation")
} }
listOf("runtimeClasspath", "testRuntimeClasspath").forEach { name -> configurations.named(name) {
  resolutionStrategy.dependencySubstitution {
    substitute(module("androidx.collection:collection")).using(module("org.jetbrains.compose.collection-internal:collection-desktop:1.6.0-beta02"))
  }
} }

tasks.register<JavaExec>("dragCheck") {
  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("render.DragCheckKt")
}

// The ViewModel needs a Context object; android-all supplies the class (only ever passed around, never used).
val androidAll by configurations.creating
dependencies { androidAll("org.robolectric:android-all:15-robolectric-12650502") }
tasks.register<JavaExec>("vmCheck") {
  classpath = sourceSets["main"].runtimeClasspath + androidAll
  mainClass.set("render.VmCheckKt")
}
