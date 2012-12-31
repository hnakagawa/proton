package proton.inject.internal.binding;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import proton.inject.ApplicationScoped;

public class BindingBuilderImpl<T> implements BindingBuilder<T> {
	private BindingImpl<T> mBinding;

	public BindingBuilderImpl(BindingImpl<T> binding) {
		mBinding = binding;
	}

	@Override
	public ScopedBuilder to(Class<? extends T> clazz) {
		mBinding.setToClass(clazz);
		return this;
	}

	@Override
	public ScopedBuilder toProvider(Class<? extends Provider<T>> provider) {
		mBinding.setProviderClass(provider);
		return this;
	}

	@Override
	public void toProvider(Provider<T> provider) {
		mBinding.setProvider(provider);
		mBinding.setScope(ApplicationScoped.class);
	}

	@Override
	public void in(Class<? extends Annotation> scope) {
		mBinding.setScope(scope);
	}
}
