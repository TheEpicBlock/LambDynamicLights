package lambdynamiclights.task

import lambdynamiclights.data.Nmt
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject

abstract class GenerateNmtTask @Inject constructor() : DefaultTask() {
	@get:Input
	abstract val nmt: Property<Nmt>

	@get:OutputDirectory
	abstract val outputDir: DirectoryProperty

	init {
		this.group = "generation"
	}

	@TaskAction
	@Throws(IOException::class)
	fun generateManifest() {
		val metaInfDir = this.outputDir.asFile.get().toPath().resolve("META-INF")
		val output = metaInfDir.resolve("neoforge.mods.toml")

		Files.createDirectories(metaInfDir)
		if (Files.exists(output)) {
			Files.delete(output)
		}

		Files.writeString(output, this.nmt.get().toToml())
	}
}
