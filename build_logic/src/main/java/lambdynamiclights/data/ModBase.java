package lambdynamiclights.data;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ModBase<SELF extends ModBase<SELF>> implements Serializable {
	@SerializedName("id")
	protected String namespace;
	protected String name;
	protected String description;
	protected String icon;

	public ModBase(String namespace, String name) {
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
}
