package proton.inject.internal;

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
import proton.inject.ApplicationScoped;
import proton.inject.ContextScoped;
import proton.inject.Injector;
import proton.inject.ProvisionException;
import proton.inject.internal.binding.Binding;
import proton.inject.internal.binding.Bindings;
import proton.inject.internal.util.ReflectionUtils;
import proton.inject.internal.util.SparseClassArray;

public class InjectorImpl implements Injector {
	private static final Object LOCK = new Object();

	private final Context mContext;
	private final InjectorImpl mApplicationInjector;

	private final Bindings mBindings;
	private final SparseClassArray<Provider<?>> mProviders = new SparseClassArray<Provider<?>>();
	private final Queue<Required> mTraversalQueue = new ArrayDeque<Required>();

	private final Provider<Injector> mInjectorProvdier = new Provider<Injector>() {
		@Override
		public Injector get() {
			return InjectorImpl.this;
		}
	};

	public InjectorImpl(Context context, Bindings bindings, InjectorImpl applicationInjector) {
		mContext = context;
		mBindings = bindings;
		mApplicationInjector = applicationInjector;
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
				if (binding == null)
					throwNoFoundBinding(key, requiredBy);

				provider = binding.getProvider();
				if (provider != null)
					return provider;

				if (!isInScope(binding)) {
					if (mApplicationInjector == null)
						throwNoFoundBinding(key, requiredBy);
					return mApplicationInjector.getProvider(key, requiredBy);
				}

				if (requiredBy != null)
					throwNoFoundBinding(key, requiredBy);

				enqueueTraversal(key, requiredBy == null ? "root" : requiredBy);
				pollTraversalQueue();

				provider = (Provider<T>) mProviders.get(key);
				if (provider == null)
					throwNoFoundBinding(key, requiredBy);
			}

			return (Provider<T>) (binding != null && binding.getProviderClass() != null ? provider.get() : provider);
		}
	}

	@Override
	public <T> T inject(T obj) {
		synchronized (LOCK) {
			Field[] fields = getFiels(obj.getClass());
			pollTraversalQueue();

			for (Field field : fields) {
				try {
					Object value = getValueOrProvider(field.getType(), field.getGenericType(), obj);
					field.set(obj, value);
				} catch (IllegalArgumentException exp) {
					throw new ProvisionException(exp.getCause());
				} catch (IllegalAccessException exp) {
					throw new ProvisionException(exp.getCause());
				}
			}
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
			addProvider(required);
		}

		if (mApplicationInjector != null)
			mApplicationInjector.pollTraversalQueue();
	}

	private void addProvider(Required required) {
		Provider<?> provider;
		if (required.key == Injector.class)
			provider = mInjectorProvdier;
		else {
			Class<?> clazz = (Class<?>) (required.binding != null ? required.binding.getToClass() : required.key);
			Field[] fields = getFiels(clazz);

			Constructor<?> constructor = getConstructor(clazz, required.requiredBy);
			Type[] types = constructor.getGenericParameterTypes();
			for (Type type : types)
				enqueueTraversal(type, constructor);
			provider = createJitProvider(constructor, types, fields, required.requiredBy);
		}

		mProviders.put(required.key, provider);
	}

	private Provider<?> createJitProvider(final Constructor<?> constructor, final Type[] types, final Field[] fields,
			final Object requiredBy) {
		return new Provider<Object>() {
			private volatile Object obj;

			public Object get() {
				if (obj == null) {
					synchronized (this) {
						if (obj == null) {
							Class<?>[] params = constructor.getParameterTypes();
							Object[] args = new Object[params.length];

							for (int i = 0; i < args.length; i++)
								args[i] = getValueOrProvider(params[i], types[i], requiredBy);

							try {
								obj = constructor.newInstance(args);
								for (int i = 0; i < fields.length; i++) {
									Object value = getValueOrProvider(fields[i].getType(), fields[i].getGenericType(),
											requiredBy);
									fields[i].set(obj, value);
								}
							} catch (IllegalAccessException exp) {
								throw new ProvisionException(exp.getCause());
							} catch (InvocationTargetException exp) {
								throw new ProvisionException(exp.getCause());
							} catch (InstantiationException exp) {
								throw new ProvisionException(exp);
							}
						}
					}
				}
				return obj;
			}
		};
	}

	private Object getValueOrProvider(Class<?> clazz, Type type, Object requiredBy) {
		Provider<?> provider = getProvider(ReflectionUtils.toActualClass(type), requiredBy);
		return Provider.class.isAssignableFrom(clazz) ? provider : provider.get();
	}

	private Field[] getFiels(Class<?> clazz) {
		List<Field> fieldsList = new ArrayList<Field>();
		for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
			for (Field field : c.getDeclaredFields()) {
				if (field.getAnnotation(javax.inject.Inject.class) == null)
					continue;

				field.setAccessible(true);
				fieldsList.add(field);
				enqueueTraversal(field.getGenericType(), field);
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

	private void enqueueTraversal(Type type, Object requiredBy) {
		Class<?> clazz = ReflectionUtils.toActualClass(type);

		Binding<?> binding = mBindings.get(clazz);
		if (binding == null) {
			if (ReflectionUtils.isAbstract(clazz) && clazz != Injector.class)
				throwNoFoundBinding(type, requiredBy);
		}

		Required req = new Required(clazz, binding, requiredBy);
		if (isInScope(binding) || (mApplicationInjector == null && clazz == Injector.class))
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

	private boolean isInScope(Binding<?> binding) {
		if (mContext instanceof Application)
			return binding != null && binding.getScope() == ApplicationScoped.class;
		return binding == null || binding.getScope() == ContextScoped.class;
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
