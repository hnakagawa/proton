package proton.inject.listener;

import proton.inject.Injector;

public interface ProviderListener {

	public void onCreateInstance(Injector injector, Object instance);
}
