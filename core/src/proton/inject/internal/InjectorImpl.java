package proton.inject.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.inject.Provider;

import android.app.Application;
import android.content.Context;
import proton.inject.Injector;
import proton.inject.ProvisionException;
import proton.inject.internal.binding.Binding;
import proton.inject.internal.binding.Bindings;
import proton.inject.internal.util.InjectorUtils;
import proton.inject.internal.util.SparseClassArray;
import proton.inject.listener.ProviderListeners;
import proton.inject.scope.ApplicationScoped;
import proton.inject.scope.Dependent;

public class InjectorImpl implements Injector {
	private static final Object LOCK = new Object();

	private final Context mContext;
	private final InjectorImpl mApplicationInjector;

	private final Bindings mBindings;
	private final ProviderListeners mProviderListeners;
	private final SparseClassArray<Provider<?>> mProviders = new SparseClassArray<Provider<?>>();
	private final Queue<Required> mTraversalQueue = new ArrayDeque<Required>();

	private final Provider<Injector> mInjectorProvdier = new Provider<Injector>() {
		@Override
		public Injector get() {
			return InjectorImpl.this;
		}
	};

	public InjectorImpl(Context context, Bindings bindings, ProviderListeners providerListeners,
			InjectorImpl applicationInjector) {
		mContext = context;
		mBindings = bindings;
		mProviderListeners = providerListeners;
		mApplicationInjector = applicationInjector;
	}

	@Override
	public ProviderListeners getProviderListeners() {
		return mProviderListeners;
	}

	@Override
	public <T> T getInstance(Class<T> key) {
		return getProvider(key).get();
	}

	@Override
	public <T> Provider<T> getProvider(Class<T> key) {
		return getProvider(key, null);
	}

	@SuppressWarnings("unchecked")
	private <T> Provider<T> getProvider(Class<T> key, Object requiredBy) {
		synchronized (LOCK) {
			Provider<T> provider = (Provider<T>) mProviders.get(key);
			Binding<T> binding = (Binding<T>) mBindings.get(key);
			if (provider == null) {
				if (binding == null && InjectorUtils.isAbstract(key))
					throwNoFoundBinding(key, requiredBy);

				if (!isInScope(key, binding)) {
					if (mApplicationInjector == null)
						throwNoFoundBinding(key, requiredBy);
					return mApplicationInjector.getProvider(key, requiredBy);
				}

				if (requiredBy != null)
					throwNoFoundBinding(key, requiredBy);

				addTraversal(key, "root");
				pollTraversalQueue();

				provider = (Provider<T>) mProviders.get(key);
				if (provider == null)
					throwNoFoundBinding(key, requiredBy);
			}

			return (Provider<T>) (provider instanceof ProviderProvider
					|| (binding != null && binding.getProviderClass() != null) ? provider.get() : provider);
		}
	}

	@Override
	public <T> T inject(T obj) {
		synchronized (LOCK) {
			Field[] fields = getFieldsAndAddTraversal(obj.getClass());
			pollTraversalQueue();
			injectFields(obj, fields, obj);
			return obj;
		}
	}

	@Override
	public Injector getApplicationInjector() {
		return mApplicationInjector == null ? this : mApplicationInjector;
	}

	@Override
	public Context getContext() {
		return mContext;
	}

	private void pollTraversalQueue() {
		Required required;
		while ((required = mTraversalQueue.poll()) != null) {
			if (mProviders.get(required.key) != null)
				continue;
			mProviders.put(required.key, createProvider(required));
		}

		if (mApplicationInjector != null)
			mApplicationInjector.pollTraversalQueue();
	}

	private Provider<?> createProvider(Required required) {
		Provider<?> provider = null;
		if (required.key == Injector.class)
			provider = mInjectorProvdier;
		else if (required.binding != null && (provider = required.binding.getProvider()) != null) {
			provider = new ApplicationProvider(provider, getFieldsAndAddTraversal(provider.getClass()));
		} else {
			Class<?> clazz = (Class<?>) (required.binding != null ? required.binding.getToClass() : required.key);
			Field[] fields = getFieldsAndAddTraversal(clazz);

			Constructor<?> constructor = getConstructor(clazz, required.requiredBy);
			Type[] types = constructor.getGenericParameterTypes();
			for (Type type : types)
				addTraversal(type, constructor);

			if (getScope(clazz, required.binding) == Dependent.class)
				provider = new DependentProvider(constructor, types, fields, required.requiredBy);
			else
				provider = createJitProvider(constructor, types, fields, required.requiredBy);

		}

		return provider;
	}

	private Provider<?> createJitProvider(final Constructor<?> constructor, final Type[] types, final Field[] fields,
			final Object requiredBy) {
		return new Provider<Object>() {
			private volatile Object obj;

			public Object get() {
				if (obj == null) {
					synchronized (this) {
						if (obj == null) {
							obj = createInstance(constructor, types, requiredBy);
							injectFields(obj, fields, requiredBy);
							mProviderListeners.call(mContext, obj);
						}
					}
				}
				return obj;
			}
		};
	}

	private Object createInstance(Constructor<?> constructor, Type[] types, Object requiredBy) {
		Class<?>[] params = constructor.getParameterTypes();
		Object[] args = new Object[params.length];

		for (int i = 0; i < args.length; i++)
			args[i] = getValueOrProvider(params[i], types[i], requiredBy);

		try {
			return constructor.newInstance(args);
		} catch (IllegalAccessException exp) {
			throw new ProvisionException(exp);
		} catch (InvocationTargetException exp) {
			throw new ProvisionException(exp);
		} catch (InstantiationException exp) {
			throw new ProvisionException(exp);
		}
	}

	private void injectFields(Object receiver, Field[] fields, Object requiredBy) {
		try {
			for (int i = 0; i < fields.length; i++) {
				Object value = getValueOrProvider(fields[i].getType(), fields[i].getGenericType(), requiredBy);
				fields[i].set(receiver, value);
			}
		} catch (IllegalAccessException exp) {
			throw new ProvisionException(exp);
		}
	}

	private Object getValueOrProvider(Class<?> clazz, Type type, Object requiredBy) {
		Provider<?> provider = getProvider(InjectorUtils.toActualClass(type), requiredBy);
		return Provider.class.isAssignableFrom(clazz) ? provider : provider.get();
	}

	private Field[] getFieldsAndAddTraversal(Class<?> clazz) {
		List<Field> fieldsList = new ArrayList<Field>();
		for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
			for (Field field : c.getDeclaredFields()) {
				if (field.getAnnotation(javax.inject.Inject.class) == null)
					continue;

				field.setAccessible(true);
				fieldsList.add(field);
				addTraversal(field.getGenericType(), field);
			}
		}

		return fieldsList.toArray(new Field[fieldsList.size()]);
	}

	private Constructor<?> getConstructor(Class<?> clazz, Object requiredBy) {
		Constructor<?> constructor = null;
		for (Constructor<?> c : clazz.getDeclaredConstructors()) {
			if (c.getAnnotation(javax.inject.Inject.class) == null)
				continue;

			if (constructor != null)
				throw new ProvisionException("Too many injectable constructors on " + clazz);

			c.setAccessible(true);
			constructor = c;
		}

		if (constructor == null) {
			try {
				constructor = clazz.getConstructor();
			} catch (NoSuchMethodException exp) {
				throw new ProvisionException(exp);
			}
		}

		return constructor;
	}

	private void addTraversal(Type type, Object requiredBy) {
		Class<?> clazz = InjectorUtils.toActualClass(type);

		Binding<?> binding = mBindings.get(clazz);
		if (binding == null && InjectorUtils.isAbstract(clazz) && clazz != Injector.class)
			throwNoFoundBinding(type, requiredBy);

		Required req = new Required(clazz, binding, requiredBy);
		if (isInScope(clazz, binding) || (mApplicationInjector == null && clazz == Injector.class))
			mTraversalQueue.add(req);
		else {
			if (mApplicationInjector == null)
				throw new ProvisionException(requiredBy + " has illegal scope dependency");
			mApplicationInjector.mTraversalQueue.add(req);
		}
	}

	private String throwNoFoundBinding(Type type, Object requiredBy) {
		throw new ProvisionException("No found binding for " + type + " required by " + requiredBy);
	}

	private boolean isInScope(Class<?> clazz, Binding<?> binding) {
		return mContext instanceof Application ^ !(ApplicationScoped.class == getScope(clazz, binding));
	}

	private Class<?> getScope(Class<?> clazz, Binding<?> binding) {
		Class<? extends Annotation> scope;
		if (binding != null && (scope = binding.getScope()) != null)
			return scope;
		return InjectorUtils.getScopeAnnotation(clazz);
	}

	private interface ProviderProvider extends Provider<Object> {
	}

	private class ApplicationProvider implements ProviderProvider {
		private volatile boolean isInjected;
		private final Provider<?> mProvider;
		private final Field[] mFields;

		ApplicationProvider(Provider<?> provider, Field[] fields) {
			mProvider = provider;
			mFields = fields;
		}

		@Override
		public Object get() {
			if (!isInjected) {
				synchronized (this) {
					if (!isInjected) {
						injectFields(mProvider, mFields, mProvider);
						isInjected = true;
						mProviderListeners.call(mContext, this);
					}
				}
			}
			return mProvider;
		}
	}

	private class DependentProvider implements ProviderProvider {
		private final Constructor<?> mConstructor;
		private final Type[] mTypes;
		private final Field[] mFields;
		private final Object mRequiredBy;

		DependentProvider(Constructor<?> constructor, Type[] types, Field[] fields, Object requiredBy) {
			mConstructor = constructor;
			mTypes = types;
			mFields = fields;
			mRequiredBy = requiredBy;
		}

		@Override
		public Object get() {
			return createJitProvider(mConstructor, mTypes, mFields, mRequiredBy);
		}
	}

	private static class Required {
		private final Class<?> key;
		private final Binding<?> binding;
		private final Object requiredBy;

		private Required(Class<?> key, Binding<?> binding, Object requiredBy) {
			this.key = key;
			this.binding = binding;
			this.requiredBy = requiredBy;
		}
	}
}
