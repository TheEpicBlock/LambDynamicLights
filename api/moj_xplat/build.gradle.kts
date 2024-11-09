import lambdynamiclights.Constants
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
	id("lambdynamiclights-common")
}

base.archivesName.set(Constants.NAME + "-api-mojmap")

dependencies {
	mappings(loom.officialMojangMappings())
}

val apiProject = project(":api")

tasks.remapJar {
	val remapJar = apiProject.tasks.named("remapJar", RemapJarTask::class)
	dependsOn(remapJar)

	classpath.setFrom((loom as LoomGradleExtension).getMinecraftJarsCollection(MappingsNamespace.INTERMEDIARY))
	inputFile.convention(remapJar.flatMap { it.archiveFile })
	sourceNamespace = "intermediary"
	targetNamespace = "named"
}

// Add the remapped JAR artifact
apiProject.configurations["mojmapApiElements"].artifacts.removeIf{
	true
}
apiProject.artifacts.add("mojmapApiElements", tasks.remapJar) {
	classifier = "mojmap"
}
apiProject.configurations["mojmapRuntimeElements"].artifacts.removeIf{
	true
}
apiProject.artifacts.add("mojmapRuntimeElements", tasks.remapJar) {
	classifier = "mojmap"
}

val remapMojmapSourcesTask = tasks.register("remapMojmapSourcesJar", RemapSourcesJarTask::class) {
	val remapJar = apiProject.tasks.named("remapSourcesJar", RemapSourcesJarTask::class)
	dependsOn(remapJar)

	classpath.setFrom((loom as LoomGradleExtension).getMinecraftJarsCollection(MappingsNamespace.INTERMEDIARY))
	inputFile.convention(remapJar.flatMap { it.archiveFile })
	archiveClassifier = "moj"
	sourceNamespace = "intermediary"
	targetNamespace = "named"
}

// Add the remapped sources artifact
apiProject.configurations["mojmapSourcesElements"].artifacts.removeIf {
	true
}
apiProject.artifacts.add("mojmapSourcesElements", remapMojmapSourcesTask) {
	classifier = "mojmap-sources"
}
