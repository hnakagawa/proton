package proton.inject.internal.binding;

public interface Bindings {
	public <T> Binding<T> get(Class<T> key);
}
