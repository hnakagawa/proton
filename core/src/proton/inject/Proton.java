package proton.inject;

import static proton.inject.internal.util.Validator.checkState;

import static proton.inject.internal.util.Validator.checkNotNull;

import java.util.WeakHashMap;

import proton.inject.internal.InjectorImpl;
import proton.inject.internal.binding.BindingsImpl;

import android.app.Application;
import android.content.Context;

public class Proton {
	private static WeakHashMap<Context, InjectorImpl> sInjectors = new WeakHashMap<Context, InjectorImpl>();
	private static BindingsImpl sBindingContainer;
	private static InjectorImpl sApplicationInjector;

	public static synchronized void initialize(Application app) {
		initialize(app, new DefaultModule());
	}

	public static synchronized void initialize(Application app, Module module) {
		checkState(sApplicationInjector == null, "Already initialized Proton");

		sBindingContainer = new BindingsImpl();
		module.configure(sBindingContainer);

		sApplicationInjector = new InjectorImpl(app, sBindingContainer, null);
		sInjectors.put(app, sApplicationInjector);
	}

	public static synchronized Injector getInjector(Context context) {
		InjectorImpl parent = checkNotNull(sInjectors.get(context.getApplicationContext()), "Proton is not initialized yet");

		InjectorImpl injector = sInjectors.get(context);
		if (injector == null) {
			injector = new InjectorImpl(context, sBindingContainer, parent);
			sInjectors.put(context, injector);
		}

		return injector;
	}
	
	public static synchronized void destroy() {
	    sBindingContainer = null;
	    sApplicationInjector = null;
	    sInjectors.clear();
	}

	public static synchronized void destroyInjector(Context context) {
		sInjectors.remove(context);
	}
}
