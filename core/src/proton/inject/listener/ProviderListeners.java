package proton.inject.listener;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class ProviderListeners {
	private List<ProviderListener> mListeners = new ArrayList<ProviderListener>();

	public void register(ProviderListener listener) {
		synchronized (this) {
			mListeners.add(listener);
		}
	}

	public void call(Context context, Object obj) {
		synchronized (this) {
			for (ProviderListener listener : mListeners)
				listener.onCreateInstance(context, obj);
		}
	}
}
