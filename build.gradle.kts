plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// Guard against KSP silently emitting no Hilt output. The Hilt Gradle plugin rewrites the
// superclass of @AndroidEntryPoint classes to Hilt_<Name> in bytecode, so a missing generated
// class still compiles and only fails at launch with NoClassDefFoundError. Fail the build right
// after KSP instead. (@HiltAndroidApp is skipped: its Hilt_ class comes from hiltJavaCompile.)
subprojects {
    plugins.withId("com.google.dagger.hilt.android") {
        plugins.withId("com.google.devtools.ksp") {
            val mainSources = file("src/main")
            val kspGenerated = layout.buildDirectory.dir("generated/ksp").get().asFile
            val modulePath = path
            tasks.matching { it.name.matches(Regex("ksp[A-Z]\\w*Kotlin")) && !it.name.contains("Test") }
                .configureEach {
                    val variant = name.removePrefix("ksp").removeSuffix("Kotlin").replaceFirstChar { it.lowercase() }
                    doLast {
                        val entryPoint = Regex("@AndroidEntryPoint\\b[\\s\\S]*?\\bclass\\s+(\\w+)")
                        val expected = mainSources.walkTopDown()
                            .filter { it.isFile && it.extension == "kt" }
                            .flatMap { f -> entryPoint.findAll(f.readText()).map { m -> m.groupValues[1] } }
                            .toSet()
                        val generated = kspGenerated.resolve("$variant/java").walkTopDown()
                            .filter { it.isFile }.map { it.name }.toSet()
                        val missing = expected.filter { "Hilt_$it.java" !in generated }
                        if (missing.isNotEmpty()) {
                            throw GradleException(
                                "$modulePath:$name produced no Hilt_ class for ${missing.joinToString()}; " +
                                    "run a clean build (stale KSP output would crash the app at launch)"
                            )
                        }
                        logger.info("$modulePath:$name Hilt entry points verified: ${expected.sorted()}")
                    }
                }
        }
    }
}
