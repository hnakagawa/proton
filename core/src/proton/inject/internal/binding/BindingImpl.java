package proton.inject.internal.binding;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import proton.inject.ContextScoped;

public class BindingImpl<T> implements Binding<T> {
	private final Class<T> mBindClass;
	private Class<?> mToClass;
	private Class<? extends Provider<T>> mProviderClass;
	private Provider<T> mProvider;
	private Class<? extends Annotation> mScope = ContextScoped.class;
	private boolean mIsImplicitScope = true;

	public BindingImpl(Class<T> key) {
		mBindClass = key;
		mToClass = key;
	}

	@Override
	public Class<T> getBindClass() {
		return mBindClass;
	}

	@Override
	public Class<?> getToClass() {
		return mProviderClass != null ? mProviderClass : mToClass;
	}

	public void setToClass(Class<?> toClass) {
		mToClass = toClass;
	}

	public void setProviderClass(Class<? extends Provider<T>> providerClass) {
		mProviderClass = providerClass;
	}

	@Override
	public Class<? extends Provider<T>> getProviderClass() {
		return mProviderClass;
	}

	public void setProvider(Provider<T> provider) {
		mProvider = provider;
	}

	@Override
	public Provider<T> getProvider() {
		return (Provider<T>) mProvider;
	}

	public void setScope(Class<? extends Annotation> scope) {
		mScope = scope;
		mIsImplicitScope = false;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return mScope;
	}

	@Override
	public boolean isImplicitScope() {
		return mIsImplicitScope;
	}
}
