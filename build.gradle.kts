import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        // Shitpack repo which contains our tools and dependencies
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        // Cloudstream gradle plugin which makes everything work and builds plugins
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) =
        extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) =
        extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        // when running through github workflow, GITHUB_REPOSITORY should contain current repository
        // name
        // you can modify it to use other git hosting services, like gitlab
        setRepo(
                System.getenv("GITHUB_REPOSITORY")
                        ?: "https://github.com/Rowdy-Avocado/Rowdycado-Extensions"
        )
    }

    android {
        namespace = "com.RowdyAvocado"

        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
            targetSdk = 35
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8) // Required
                // Disables some unnecessary features
                freeCompilerArgs.addAll(
                        "-Xno-call-assertions",
                        "-Xno-param-assertions",
                        "-Xno-receiver-assertions"
                )
            }
        }
    }

    dependencies {
        val cloudstream by configurations
        val implementation by configurations

        // Stubs for all Cloudstream classes
        cloudstream("com.lagradost:cloudstream3:pre-release")

        // these dependencies can include any of those which are added by the app,
        // but you dont need to include any of them if you dont need them
        // https://github.com/recloudstream/cloudstream/blob/master/app/build.gradle.kts
        implementation(kotlin("stdlib")) // adds standard kotlin features
        implementation("com.github.Blatzar:NiceHttp:0.4.11") // http library
        implementation("org.jsoup:jsoup:1.18.3") // html parser
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
        implementation("io.karn:khttp-android:0.1.2")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
        implementation("com.faendir.rhino:rhino-android:1.6.0")
        implementation("com.google.code.gson:gson:2.10")
    }

    // This task will be added to every subproject (each of your extensions)
    tasks.register("pushToDevice") {
        group = "Cloudstream"
        description = "Builds the plugin and pushes it to the connected Android device via ADB."

        // This ensures that the 'make' task for the specific subproject runs first.
        dependsOn(tasks.named("make"))

        doLast {
            // Dynamically gets the name of the project (e.g., "EXName")
            val pluginFile = File(buildDir, "${project.name}.cs3")

            if (pluginFile.exists()) {
                println("✅ Plugin built successfully for ${project.name}.")
                println("🅿️ Pushing ${pluginFile.name} to device...")

                // Execute the adb push command.
                exec {
                    commandLine(
                            "adb",
                            "push",
                            pluginFile.absolutePath,
                            "/sdcard/Cloudstream3/plugins/"
                    )
                }
                // force-stop Cloudstream
                exec {
                    commandLine(
                        "adb",
                        "shell",
                        "am",
                        "force-stop",
                        "com.lagradost.cloudstream3"
                    )
                }

                // restart Cloudstream
                exec {
                    commandLine(
                        "adb",
                        "shell",
                        "monkey",
                        "-p",
                        "com.lagradost.cloudstream3",
                        "-c",
                        "android.intent.category.LAUNCHER",
                        "1"
                    )
                }

                println("🚀 Push successful. The plugin should now be in Cloudstream.")
            } else {
                throw GradleException(
                        "❌ Build failed: Plugin file not found at ${pluginFile.absolutePath}"
                )
            }
        }
    }
}

task<Delete>("clean") { delete(rootProject.layout.buildDirectory) }
