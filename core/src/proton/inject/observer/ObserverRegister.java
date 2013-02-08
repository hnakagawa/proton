package proton.inject.observer;

import android.app.Application;
import proton.inject.Injector;
import proton.inject.listener.ProviderListener;

public class ObserverRegister implements ProviderListener {
	@Override
	public void onCreateInstance(Injector injector, Object obj) {
		if (!(injector.getContext() instanceof Application))
			injector.getInstance(ObserverManager.class).registerIfObserver(obj);
	}
}
