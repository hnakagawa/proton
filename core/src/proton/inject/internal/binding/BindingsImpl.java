package proton.inject.internal.binding;

import proton.inject.ConfigurationException;
import proton.inject.internal.util.SparseClassArray;

public class BindingsImpl implements Bindings {
	private SparseClassArray<Binding<?>> mBindings = new SparseClassArray<Binding<?>>();

	public <T> void add(Binding<T> binding) {
		Class<T> key = binding.getBindClass();
		if (mBindings.get(key) != null)
			throw new ConfigurationException(key.getName() + " was already configured");

		mBindings.put(binding.getToClass(), binding);
	}

	@SuppressWarnings("unchecked")
	public <T> Binding<T> get(Class<T> key) {
		return (BindingImpl<T>) mBindings.get(key);
	}
}
