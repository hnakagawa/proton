package proton.inject.internal.binding;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import static proton.inject.internal.util.Validator.checkNotNull;

import proton.inject.internal.util.InjectorUtils;
import proton.inject.scope.ApplicationScoped;
import proton.inject.scope.ContextScoped;
import proton.inject.scope.Dependent;

public class BindingBuilderImpl<T> implements BindingBuilder<T> {
	private final Binding<T> mBinding;

	public BindingBuilderImpl(Binding<T> binding) {
		mBinding = binding;
		Class<?> clazz = binding.getBindClass();
		if (!InjectorUtils.isAbstract(clazz))
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
		setScope(InjectorUtils.toActualClass(provider));
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
				|| (ann = clazz.getAnnotation(Dependent.class)) != null
				|| (ann = clazz.getAnnotation(ContextScoped.class)) != null)
			mBinding.setScope(ann.annotationType());
	}
}
