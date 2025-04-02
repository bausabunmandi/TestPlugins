rootProject.name = "CloudstreamPlugins"

// This file sets what projects are included. Every time you add a new project, you must add it
// to the includes below.

val disabled = listOf<String>()

File(rootDir, ".").eachDir { dir ->
    if (!disabled.contains(dir.name) && File(dir, "build.gradle.kts").exists()) {
        include(dir.name)
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}

// Plugins are included like this
// include(
//     "ExampleProvider"
// )
