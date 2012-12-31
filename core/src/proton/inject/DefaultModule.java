package proton.inject;

import static proton.inject.internal.util.Validator.checkNotNull;
import android.content.Context;

import static proton.inject.internal.util.Validator.checkState;

import proton.inject.internal.binding.BindingBuilder;
import proton.inject.internal.binding.BindingBuilderImpl;
import proton.inject.internal.binding.BindingImpl;
import proton.inject.internal.binding.BindingsImpl;
import proton.inject.internal.provider.ContextProvider;

public class DefaultModule implements Module {
	private BindingsImpl mBindingContainer;

	@Override
	public final synchronized void configure(BindingsImpl bindingContainer) {
		checkState(mBindingContainer == null, "Re-entry is not allowed.");

		mBindingContainer = checkNotNull(bindingContainer, "BindingContainer");
		try {
			configure();
		} finally {
			mBindingContainer = null;
		}
	}

	protected void configure() {
		bind(Context.class).toProvider(ContextProvider.class);
	}

	protected <T> BindingBuilder<T> bind(Class<T> clazz) {
		checkState(mBindingContainer != null, "The BindingContainer can only be used inside configure()");
		BindingImpl<T> binding = new BindingImpl<T>(clazz);
		mBindingContainer.add(binding);
		return new BindingBuilderImpl<T>(binding);
	}
}
