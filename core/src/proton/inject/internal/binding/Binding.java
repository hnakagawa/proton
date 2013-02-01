package proton.inject.internal.binding;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

public interface Binding<T> {
	public Class<T> getBindClass();

	public Class<?> getToClass();

	public Provider<T> getProvider();

	public Class<? extends Provider<T>> getProviderClass();

	public Class<? extends Annotation> getScope();
}
