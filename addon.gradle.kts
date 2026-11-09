// Extra build logic, applied on top of build.gradle.kts (see dependencies.gradle for the main dependency list).
// Accessors such as testImplementation(...) are not generated for addon scripts, so configurations are
// addressed by name when needed.

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
val mainSourceSet = sourceSets.getByName("main")

// ---------------------------------------------------------------------------------------------
// Unit tests (JUnit 4 + Mockito) and coverage (JaCoCo)
// ---------------------------------------------------------------------------------------------
// Coverage is produced by every `test` run. `./gradlew jacocoTestReport` only assembles the HTML/XML
// report, `./gradlew jacocoTestCoverageVerification` additionally enforces the floor defined below.
apply(plugin = "jacoco")

project.extensions.configure(JacocoPluginExtension::class.java) {
    toolVersion = "0.8.16"
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isEnabled = true
    }
}

// A test-only mod + in-game assertions. It is compiled into its own source set and only added to the
// run classpath when -PgameTest (or the gameTestServer task) is used, so it never ships in the mod jar.
val gameTestSourceSet = sourceSets.create("gameTest")
configurations.getByName(gameTestSourceSet.compileClasspathConfigurationName)
    .extendsFrom(configurations.getByName("compileClasspath"))
configurations.getByName(gameTestSourceSet.runtimeClasspathConfigurationName)
    .extendsFrom(configurations.getByName("runtimeClasspath"))
gameTestSourceSet.compileClasspath += mainSourceSet.output
gameTestSourceSet.compileClasspath += mainSourceSet.compileClasspath
gameTestSourceSet.runtimeClasspath += mainSourceSet.output
gameTestSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath
// The game-test mod is plain Java 8. Leaving Jabel's compiler plugin on this source set's classpath
// makes javac auto-load it through ServiceLoader, which then needs ByteBuddy's agent (not present here).
gameTestSourceSet.compileClasspath = gameTestSourceSet.compileClasspath
    .filter { !it.name.contains("jabel") }

// The Lua test program lives next to the game-test mod, but the headless Lua test must be able to find
// it from the test runtime classpath. Adding the directory as an extra test resource root keeps a
// single copy of the script; test resources are not part of the published jar.
sourceSets.getByName("test").resources.srcDir("src/gameTest/resources")

tasks.named("check").configure {
    dependsOn(gameTestSourceSet.classesTaskName)
}

val isGameTestRun = project.hasProperty("gameTest") ||
    gradle.startParameter.taskNames.any { it.contains("gameTestServer") }
if (isGameTestRun) {
    tasks.withType<JavaExec>().configureEach {
        if (name.startsWith("runServer") || name.startsWith("runClient")) {
            classpath += gameTestSourceSet.output
            systemProperty("openperipheral.integration.gameTest", "true")
        }
    }
}

tasks.register("gameTestServer") {
    group = "verification"
    description = "Boots a dedicated server, runs the in-game integration test mod, and exits with its result"
    if (isGameTestRun) dependsOn(tasks.named("runServer25"))
}

fun jacocoClassDirs() = project.files(mainSourceSet.output.classesDirs).asFileTree.matching {
    // Generated token holder only contains build version constants; measuring it would be noise.
    exclude("openperipheral/integration/Tags.class")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates HTML and XML JaCoCo coverage reports from the unit test run"
    dependsOn(tasks.named("test"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(jacocoClassDirs())
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(layout.buildDirectory.file("jacoco/test.exec"))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Fails the build when unit test line coverage drops below the configured floor"
    dependsOn(tasks.named("test"))

    classDirectories.setFrom(jacocoClassDirs())
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(layout.buildDirectory.file("jacoco/test.exec"))

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                // Regression guard, not a target. Most of this mod is thin reflection glue over
                // third-party APIs that cannot be instantiated headlessly; raise as coverage grows.
                // Current: ~54.9 % line / ~44.7 % branch (177 tests).
                minimum = "0.50".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.40".toBigDecimal()
            }
        }
    }
}

// Make the normal `check` lifecycle enforce the floor, so CI catches coverage regressions.
tasks.named("check").configure {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

// Mockito 4 generates mocks with ByteBuddy, whose agent/class-file support is only guaranteed up to the
// JVM the release was tested against. The mod targets Java 8 anyway, so run the tests on a Java 8
// toolchain instead of the Gradle daemon's JVM.
tasks.withType(Test::class.java).configureEach {
    javaLauncher = project.extensions.getByType(JavaToolchainService::class.java)
        .launcherFor {
            languageVersion = JavaLanguageVersion.of(8)
        }
}

// ---------------------------------------------------------------------------------------------
// Run-task forwarding (ComputerCraft 3.3.x / lwjgl3ify)
// ---------------------------------------------------------------------------------------------
// ComputerCraft 3.3.1 declares `required-after:lwjgl3ify` in its mcmod.info, and lwjgl3ify needs a modern
// JVM. GTNHGradle's plain runServer/runClient use the Java 8 toolchain and carry no lwjgl3ify on the
// classpath, so they die at MissingModsException before the game starts. Forward them to the modern
// variants (which GTNHGradle wires to lwjgl3ify and a Java 17+/21/25 JVM), so both `./gradlew
// runClient`/`runServer` and CI's server smoke test keep working. Mirrors the same workaround in
// OpenPeripheral.
mapOf("runServer" to "runServer25", "runClient" to "runClient25").forEach { (plain, modern) ->
    tasks.matching { it.name == plain }.configureEach {
        if (project.tasks.names.contains(modern)) {
            description = "Alias for $modern - ComputerCraft 3.3.1 requires lwjgl3ify, which needs Java 17+"
            dependsOn(modern)
            onlyIf("the plain task forwards to $modern") { false }
        }
    }
}
