package proton.inject;

import static proton.inject.internal.util.Validator.checkState;

import static proton.inject.internal.util.Validator.checkNotNull;

import java.util.Map;
import java.util.WeakHashMap;

import proton.inject.internal.InjectorImpl;
import proton.inject.internal.binding.Bindings;
import proton.inject.listener.ProviderListeners;

import android.app.Application;
import android.content.Context;

public final class Proton {
	private static Map<Context, InjectorImpl> sInjectors;
	private static Bindings sBindings;
	private static ProviderListeners sFieldInjectionListeners;

	private Proton() {
	}

	public static void initialize(Application app) {
		initialize(app, new DefaultModule());
	}

	public static void initialize(Application app, Module module) {
		synchronized (Proton.class) {
			checkState(sInjectors == null, "Already initialized Proton");
			sInjectors = new WeakHashMap<Context, InjectorImpl>();
			sBindings = new Bindings();
			sFieldInjectionListeners = new ProviderListeners();

			module.configure(sBindings, sFieldInjectionListeners);

			InjectorImpl injector = new InjectorImpl(app, sBindings, sFieldInjectionListeners, null);
			sInjectors.put(app, injector);
		}
	}

	public static Injector getInjector(Context context) {
		checkInitialize();
		InjectorImpl injector = sInjectors.get(context);
		if (injector == null) {
			synchronized (Proton.class) {
				injector = sInjectors.get(context);
				if (injector == null) {
					InjectorImpl parent = sInjectors.get(context.getApplicationContext());
					injector = new InjectorImpl(context, sBindings, sFieldInjectionListeners, parent);
					sInjectors.put(context, injector);
				}
			}
		}
		return injector;
	}

	public static void destroy() {
		synchronized (Proton.class) {
			checkInitialize();
			sFieldInjectionListeners = null;
			sBindings = null;
			sInjectors = null;
		}
	}

	public static void destroyInjector(Context context) {
		synchronized (Proton.class) {
			checkInitialize();
			sInjectors.remove(context);
		}
	}

	private static void checkInitialize() {
		checkNotNull(sInjectors, "Proton is not initialized yet");
	}
}
