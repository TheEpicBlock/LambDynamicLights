package lambdynamiclights.data;

import java.io.Serializable;

public final class Contact implements Serializable {
	private String homepage;
	private String sources;
	private String issues;

	public Contact withHomepage(String homepage) {
		this.homepage = homepage;
		return this;
	}

	public Contact withSources(String sources) {
		this.sources = sources;
		return this;
	}

	public Contact withIssues(String issues) {
		this.issues = issues;
		return this;
	}

	public String homepage() {
		return this.homepage;
	}

	public String sources() {
		return this.sources;
	}

	public String issues() {
		return this.issues;
	}

	public Contact copy() {
		return new Contact()
				.withHomepage(this.homepage)
				.withSources(this.sources)
				.withIssues(this.issues);
	}
}
