package proton.inject.listener;

import java.util.ArrayList;
import java.util.List;

import proton.inject.Injector;

public class ProviderListeners {
	private List<ProviderListener> mListeners = new ArrayList<ProviderListener>();

	public void register(ProviderListener listener) {
		synchronized (this) {
			mListeners.add(listener);
		}
	}

	public void call(Injector injector, Object obj) {
		synchronized (this) {
			for (ProviderListener listener : mListeners)
				listener.onCreateInstance(injector, obj);
		}
	}
}
