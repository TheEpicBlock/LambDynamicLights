import lambdynamiclights.Constants

plugins {
	id("lambdynamiclights")
}

val prettyName = "${Constants.PRETTY_NAME} (API)"

base.archivesName.set(Constants.NAME + "-api")

tasks.generateFmj.configure {
	this.fmj.get()
		.withNamespace(Constants.NAMESPACE + "_api")
		.withName(prettyName)
		.withDescription(Constants.API_DESCRIPTION)
		.withModMenu {
			it.withBadges("library")
				.withParent(Constants.NAMESPACE, Constants.PRETTY_NAME) { parent ->
					parent.withDescription(Constants.DESCRIPTION)
						.withIcon("assets/${Constants.NAMESPACE}/icon.png")
				}
		}
}

// Configure the maven publication.
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])

			artifactId = "lambdynamiclights-api"

			pom {
				name.set(prettyName)
				description.set(Constants.API_DESCRIPTION)
			}
		}
	}
}
