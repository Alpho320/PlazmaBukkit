import io.papermc.paperweight.util.*

plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":plazma-api")) // Purpur
    implementation("io.papermc.paper:paper-mojangapi:1.19.4-R0.1-SNAPSHOT") // Purpur
    // Paper start
    implementation("org.jline:jline-terminal-jansi:3.23.0") // Plazma - Bump Dependencies
    implementation("net.minecrell:terminalconsoleappender:1.3.0")
    /*
          Required to add the missing Log4j2Plugins.dat file from log4j-core
          which has been removed by Mojang. Without it, log4j has to classload
          all its classes to check if they are plugins.
          Scanning takes about 1-2 seconds so adding this speeds up the server start.
     */
    implementation("org.apache.logging.log4j:log4j-core:2.20.0") // Paper - implementation // Plazma - Bump Dependencies
    annotationProcessor("org.apache.logging.log4j:log4j-core:2.20.0") // Paper - Needed to generate meta for our Log4j plugins // Plazma - Bump Dependencies
    implementation("io.netty:netty-codec-haproxy:4.1.97.Final") // Paper - Add support for proxy protocol // Plazma - Bump Dependencies
    // Paper end
    implementation("org.apache.logging.log4j:log4j-iostreams:2.20.0") // Paper - remove exclusion // Plazma - Bump Dependencies
    implementation("org.ow2.asm:asm:9.5") // Plazma - Bump Dependencies
    implementation("org.ow2.asm:asm-commons:9.5") // Paper - ASM event executor generation // Plazma - Bump Dependencies
    testImplementation("org.mockito:mockito-core:5.5.0") // Paper - switch to mockito // Plazma - Bump Dependencies
    implementation("org.spongepowered:configurate-yaml:4.1.2") // Paper - config files
    implementation("commons-lang:commons-lang:2.6")
    implementation("net.fabricmc:mapping-io:0.4.2") // Paper - needed to read mappings for stacktrace deobfuscation // Plazma - Bump Dependencies
    runtimeOnly("org.xerial:sqlite-jdbc:3.41.2.2") // Plazma - Bump Dependencies
    runtimeOnly("com.mysql:mysql-connector-j:8.0.33") // Plazma - Bump Dependencies
    runtimeOnly("com.lmax:disruptor:3.4.4") // Paper
    // Paper start - Use Velocity cipher
    implementation("com.velocitypowered:velocity-native:3.2.0-SNAPSHOT") { // Plazma - Bump Dependencies
        isTransitive = false
    }
    // Paper end

    implementation("org.mozilla:rhino-runtime:1.7.14") // Purpur
    implementation("org.mozilla:rhino-engine:1.7.14") // Purpur
    implementation("dev.omega24:upnp4j:1.0") // Purpur

    runtimeOnly("org.apache.maven:maven-resolver-provider:3.9.4") // Plazma - Bump Dependencies
    runtimeOnly("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.15") // Plazma - Bump Dependencies
    runtimeOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.9.15") // Plazma - Bump Dependencies

    // Pufferfish start
    implementation("org.yaml:snakeyaml:1.33") // Plazma - Bump Dependencies
    implementation ("me.carleslc.Simple-YAML:Simple-Yaml:1.8.4") { // Plazma - Bump Dependencies
        exclude(group="org.yaml", module="snakeyaml")
    }
    // Pufferfish end

    testImplementation("io.github.classgraph:classgraph:4.8.162") // Paper - mob goal test // Plazma - Bump Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2") // Plazma - Bump Dependencies

    implementation("io.netty:netty-all:4.1.97.Final"); // Paper - Bump netty // Plazma - Bump Dependencies
}

val craftbukkitPackageVersion = "1_19_R3" // Paper

// Pufferfish Start
tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("--add-modules=jdk.incubator.vector")
}
// Pufferfish End

tasks.jar {
    archiveClassifier.set("dev")

    manifest {
        val git = Git(rootProject.layout.projectDirectory.path)
        val gitHash = "f91ea41"
        val implementationVersion = System.getenv("BUILD_NUMBER") ?: "\"$gitHash\""
        val date = "25022024" // Paper
        val gitBranch = "master" // Paper
        attributes(
            "Main-Class" to "org.bukkit.craftbukkit.Main",
            "Implementation-Title" to "CraftBukkit",
            "Implementation-Version" to "git-Plazma-$implementationVersion", // Pufferfish // Purpur // Plazma
            "Implementation-Vendor" to date, // Paper
            "Specification-Title" to "Bukkit",
            "Specification-Version" to project.version,
            "Specification-Vendor" to "Bukkit Team",
            "Git-Branch" to gitBranch, // Paper
            "Git-Commit" to gitHash, // Paper
            "CraftBukkit-Package-Version" to craftbukkitPackageVersion, // Paper
        )
        for (tld in setOf("net", "com", "org")) {
            attributes("$tld/bukkit", "Sealed" to true)
        }
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifact(tasks.shadowJar)
    }
}

relocation {
    // Order matters here - e.g. craftbukkit proper must be relocated before any of the libs are relocated into the cb package
    relocate("org.bukkit.craftbukkit" to "org.bukkit.craftbukkit.v$craftbukkitPackageVersion") {
        exclude("org.bukkit.craftbukkit.Main*")
    }
}

tasks.shadowJar {
    configurations = listOf(project.configurations.vanillaServer.get())
    archiveClassifier.set("mojang-mapped")

    for (relocation in relocation.relocations.get()) {
        relocate(relocation.fromPackage, relocation.toPackage) {
            for (exclude in relocation.excludes) {
                exclude(exclude)
            }
        }
    }
}

// Paper start
val scanJar = tasks.register("scanJarForBadCalls", io.papermc.paperweight.tasks.ScanJarForBadCalls::class) {
    badAnnotations.add("Lio/papermc/paper/annotation/DoNotUse;")
    jarToScan.set(tasks.shadowJar.flatMap { it.archiveFile })
    classpath.from(configurations.compileClasspath)
}
tasks.check {
    dependsOn(scanJar)
}
// Paper end

// Paper start - include reobf mappings in jar for stacktrace deobfuscation
val includeMappings = tasks.register<io.papermc.paperweight.tasks.IncludeMappings>("includeMappings") {
    inputJar.set(tasks.fixJarForReobf.flatMap { it.outputJar })
    mappings.set(tasks.reobfJar.flatMap { it.mappingsFile })
    mappingsDest.set("META-INF/mappings/reobf.tiny")
}

tasks.reobfJar {
    inputJar.set(includeMappings.flatMap { it.outputJar })
}
// Paper end - include reobf mappings in jar for stacktrace deobfuscation

tasks.test {
    exclude("org/bukkit/craftbukkit/inventory/ItemStack*Test.class")
}

fun TaskContainer.registerRunTask(
    name: String,
    block: JavaExec.() -> Unit
): TaskProvider<JavaExec> = register<JavaExec>(name) {
    group = "paperweight" // Purpur
    mainClass.set("org.bukkit.craftbukkit.Main")
    standardInput = System.`in`
    workingDir = rootProject.layout.projectDirectory
        .dir(providers.gradleProperty("paper.runWorkDir").getOrElse("run"))
        .asFile
    javaLauncher.set(project.javaToolchains.defaultJavaLauncher(project))

    if (rootProject.childProjects["test-plugin"] != null) {
        val testPluginJar = rootProject.project(":test-plugin").tasks.jar.flatMap { it.archiveFile }
        inputs.file(testPluginJar)
        args("-add-plugin=${testPluginJar.get().asFile.absolutePath}")
    }

    args("--nogui")
    systemProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", true)
    if (providers.gradleProperty("paper.runDisableWatchdog").getOrElse("false") == "true") {
        systemProperty("disable.watchdog", true)
    }

    val memoryGb = providers.gradleProperty("paper.runMemoryGb").getOrElse("2")
    minHeapSize = "${memoryGb}G"
    maxHeapSize = "${memoryGb}G"

    doFirst {
        workingDir.mkdirs()
    }

    block(this)
}

val runtimeClasspathWithoutVanillaServer = configurations.runtimeClasspath.flatMap { it.elements }
    .zip(configurations.vanillaServer.map { it.singleFile.absolutePath }) { runtime, vanilla ->
        runtime.filterNot { it.asFile.absolutePath == vanilla }
    }

tasks.registerRunTask("runShadow") {
    description = "Spin up a test server from the shadowJar archiveFile"
    classpath(tasks.shadowJar.flatMap { it.archiveFile })
    classpath(runtimeClasspathWithoutVanillaServer)
}

tasks.registerRunTask("runReobf") {
    description = "Spin up a test server from the reobfJar output jar"
    classpath(tasks.reobfJar.flatMap { it.outputJar })
    classpath(runtimeClasspathWithoutVanillaServer)
}

tasks.registerRunTask("runDev") {
    description = "Spin up a non-relocated Mojang-mapped test server"
    classpath(sourceSets.main.map { it.runtimeClasspath })
}
