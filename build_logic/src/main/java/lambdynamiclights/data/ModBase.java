package lambdynamiclights.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ModBase<SELF extends ModBase<SELF>> extends ModShell<SELF> {
	protected final String version;
	protected final List<String> authors = new ArrayList<>();
	protected Contact contact;
	protected String license;

	public ModBase(String namespace, String name, String version) {
		super(namespace, name);
		this.version = version;
	}

	@SuppressWarnings("unchecked")
	private SELF $self() {
		return (SELF) this;
	}

	public SELF withAuthors(List<String> authors) {
		this.authors.addAll(authors);
		return this.$self();
	}

	public SELF withAuthors(String... authors) {
		return this.withAuthors(Arrays.asList(authors));
	}

	private Contact useContact() {
		if (this.contact == null) this.contact = new Contact();
		return this.contact;
	}

	public SELF withContact(Consumer<Contact> action) {
		action.accept(this.useContact());
		return this.$self();
	}

	public SELF withLicense(String license) {
		this.license = license;
		return this.$self();
	}

	public <VARIANT extends ModBase<VARIANT>> VARIANT derive(ModBaseFactory<VARIANT> factory) {
		var variant = factory.create(this.namespace, this.name, this.version);
		this.copyTo(variant);
		variant.authors.addAll(this.authors);
		variant.contact = this.contact != null ? this.contact.copy() : null;
		variant.license = this.license;
		return variant;
	}

	public interface ModBaseFactory<SELF extends ModBase<SELF>> {
		SELF create(String namespace, String name, String version);
	}
}
