package proton.inject;

import static proton.inject.internal.util.Validator.checkNotNull;
import static proton.inject.internal.util.Validator.checkState;

import android.content.Context;

import proton.inject.internal.binding.BindingBuilder;
import proton.inject.internal.binding.BindingBuilderImpl;
import proton.inject.internal.binding.Binding;
import proton.inject.internal.binding.Bindings;
import proton.inject.internal.provider.ContextProvider;

public class DefaultModule implements Module {
	private Bindings mBindings;

	@Override
	public final synchronized void configure(Bindings bindings) {
		checkState(mBindings == null, "Re-entry is not allowed.");

		mBindings = checkNotNull(bindings, "bindings");
		try {
			configure();
		} finally {
			mBindings = null;
		}
	}

	protected void configure() {
		bind(Context.class).toProvider(ContextProvider.class);
	}

	protected <T> BindingBuilder<T> bind(Class<T> clazz) {
		checkState(mBindings != null, "The Bindings can only be used inside configure()");
		Binding<T> binding = new Binding<T>(clazz);
		mBindings.add(binding);
		return new BindingBuilderImpl<T>(binding);
	}
}
