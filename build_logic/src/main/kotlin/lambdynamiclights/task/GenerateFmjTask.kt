package lambdynamiclights.task

import com.google.gson.FormattingStyle
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lambdynamiclights.data.Fmj
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject

abstract class GenerateFmjTask @Inject constructor() : DefaultTask() {
	@get:Input
	abstract val fmj: Property<Fmj>

	@get:OutputDirectory
	abstract val outputDir: DirectoryProperty

	init {
		this.group = "generation"
	}

	@TaskAction
	@Throws(IOException::class)
	fun generateManifest() {
		val output = this.outputDir.asFile.get().toPath().resolve("fabric.mod.json")

		if (Files.exists(output)) {
			Files.delete(output)
		}

		Files.writeString(output, GSON.toJson(this.fmj.get()))
	}

	companion object {
		val GSON: Gson = GsonBuilder()
			.registerTypeAdapter(Fmj::class.java, Fmj.Serializer())
			.setFormattingStyle(FormattingStyle.PRETTY.withIndent("\t"))
			.create()
	}
}
