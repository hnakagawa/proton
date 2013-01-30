package proton.inject.internal.binding;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import static proton.inject.internal.util.Validator.checkNotNull;

import proton.inject.ApplicationScoped;
import proton.inject.Dependent;
import proton.inject.internal.util.ReflectionUtils;

public class BindingBuilderImpl<T> implements BindingBuilder<T> {
	private final BindingImpl<T> mBinding;

	public BindingBuilderImpl(BindingImpl<T> binding) {
		mBinding = binding;
		Class<?> clazz = binding.getBindClass();
		if (!ReflectionUtils.isAbstract(clazz))
			setScope(clazz);
	}

	@Override
	public ScopedBuilder to(Class<? extends T> to) {
		mBinding.setToClass(checkNotNull(to, "to"));
		setScope(to);
		return this;
	}

	@Override
	public ScopedBuilder toProvider(Class<? extends Provider<T>> provider) {
		mBinding.setProviderClass(checkNotNull(provider, "provider"));
		setScope(ReflectionUtils.toActualClass(provider));
		return this;
	}

	@Override
	public void toProvider(Provider<T> provider) {
		mBinding.setProvider(checkNotNull(provider, "provider"));
		mBinding.setScope(ApplicationScoped.class);
	}

	@Override
	public void in(Class<? extends Annotation> scope) {
		mBinding.setScope(checkNotNull(scope, "scope"));
	}

	private void setScope(Class<?> clazz) {
		Annotation ann;
		if ((ann = clazz.getAnnotation(ApplicationScoped.class)) != null
				|| (ann = clazz.getAnnotation(Dependent.class)) != null)
			mBinding.setScope(ann.annotationType());
	}
}
