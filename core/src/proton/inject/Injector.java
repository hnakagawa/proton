package proton.inject;

import javax.inject.Provider;

import android.content.Context;

public interface Injector {
	public <T> T getInstance(Class<T> key);

	public <T> Provider<T> getProvider(Class<T> key);

	public <T> T inject(T obj);

	public Injector getApplicationInjector();

	public Context getContext();
}
