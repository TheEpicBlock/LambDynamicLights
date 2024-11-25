package lambdynamiclights.data;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ModShell<SELF extends ModShell<SELF>> implements Serializable {
	@SerializedName("id")
	protected String namespace;
	protected String name;
	protected String description;
	protected String icon;

	public ModShell(String namespace, String name) {
		this.namespace = namespace;
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	private SELF $self() {
		return (SELF) this;
	}

	public SELF withNamespace(String namespace) {
		this.namespace = namespace;
		return this.$self();
	}

	public SELF withName(String name) {
		this.name = name;
		return this.$self();
	}

	public SELF withDescription(String description) {
		this.description = description;
		return this.$self();
	}

	public SELF withIcon(String icon) {
		this.icon = icon;
		return this.$self();
	}

	public void copyTo(ModShell<?> target) {
		target.namespace = this.namespace;
		target.name = this.name;
		target.description = this.description;
		target.icon = this.icon;
	}
}
