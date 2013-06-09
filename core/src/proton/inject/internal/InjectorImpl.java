package proton.inject.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Queue;

import javax.inject.Provider;

import android.app.Application;
import android.content.Context;
import proton.inject.Injector;
import proton.inject.ProvisionException;
import proton.inject.binding.Binding;
import proton.inject.binding.Bindings;
import proton.inject.internal.Element.ElementConstructor;
import proton.inject.internal.Element.ElementField;
import proton.inject.listener.FieldListeners;
import proton.inject.listener.ProviderListeners;
import proton.inject.scope.ApplicationScoped;
import proton.inject.scope.Dependent;
import proton.inject.util.ArrayDeque;
import proton.inject.util.InjectorUtils;
import proton.inject.util.SparseClassArray;

public class InjectorImpl implements Injector {
	private static final Object LOCK = new Object();

	private static final SparseClassArray<Element> mElements = new SparseClassArray<Element>();

	private final Context mContext;

	private final InjectorImpl mApplicationInjector;

	private final Bindings mBindings;
	private final ProviderListeners mProviderListeners;
	private final FieldListeners mFieldListeners;

	private final SparseClassArray<Provider<?>> mProviders = new SparseClassArray<Provider<?>>();

	private final Queue<Required> mTraversalQueue = new ArrayDeque<Required>();

	private final Provider<Injector> mInjectorProvdier = new Provider<Injector>() {
		@Override
		public Injector get() {
			return InjectorImpl.this;
		}
	};

	public InjectorImpl(Context context, Bindings bindings, ProviderListeners providerListeners,
			FieldListeners fieldListeners, InjectorImpl applicationInjector) {
		mContext = context;
		mBindings = bindings;
		mProviderListeners = providerListeners;
		mFieldListeners = fieldListeners;
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
			Element element = getElement(key);

			if (provider == null) {
				if (element.binding == null && element.isAbstract())
					throwNoFoundBinding(key, requiredBy);

				if (!isInScope(element)) {
					if (mApplicationInjector == null)
						throwNoFoundBinding(key, requiredBy);
					return mApplicationInjector.getProvider(key, requiredBy);
				}

				if (requiredBy != null)
					throwNoFoundBinding(key, requiredBy);

				addTraversal(element, "root");
				pollTraversalQueue();

				provider = (Provider<T>) mProviders.get(key);
				if (provider == null)
					throwNoFoundBinding(key, requiredBy);
			}

			return (Provider<T>) (provider instanceof ProviderProvider
					|| (element.binding != null && element.binding.getProviderClass() != null) ? provider.get()
					: provider);
		}
	}

	private Element getElement(Class<?> clazz) {
		Element element = mElements.get(clazz);
		if (element == null) {
			Binding<?> binding = (Binding<?>) mBindings.get(clazz);
			element = new Element(clazz, binding);
			mElements.put(clazz, element);
		}
		return element;
	}

	@Override
	public <T> T inject(T obj) {
		Element element;
		ElementField[] fields;
		synchronized (LOCK) {
			element = getElement(obj.getClass());
			fields = element.getElementFields(mFieldListeners);
			traverse(fields);
			pollTraversalQueue();
		}

		injectFields(obj, element, fields, element.binding, obj);
		return obj;
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
			if (mProviders.get(required.element.clazz) != null)
				continue;
			mProviders.put(required.element.clazz, createProvider(required));
		}

		if (mApplicationInjector != null)
			mApplicationInjector.pollTraversalQueue();
	}

	private Provider<?> createProvider(Required required) {
		Provider<?> provider = null;
		if (required.element.clazz == Injector.class)
			provider = mInjectorProvdier;
		else if (required.element.binding != null && (provider = required.element.binding.getProvider()) != null) {
			Element element = getElement(provider.getClass());
			ElementField[] fields = element.getElementFields(mFieldListeners);
			traverse(fields);
			provider = new ApplicationProvider(required.element, provider, fields, required.element.binding);
		} else {
			Element element = required.element.binding != null ? getElement(required.element.binding.getToClass())
					: required.element;
			ElementField[] fields = element.getElementFields(mFieldListeners);
			traverse(fields);

			ElementConstructor constructor = element.getElementConstructor();
			Type[] types = constructor.constructor.getGenericParameterTypes();
			for (Type type : types)
				addTraversal(type, constructor.constructor);

			if (getScope(required.element) == Dependent.class)
				provider = new DependentProvider(required.element, constructor, types, fields, required.requiredBy);
			else
				provider = createJitProvider(required.element, constructor, fields, types, required.requiredBy);
		}

		return provider;
	}

	private Provider<?> createJitProvider(final Element element, final ElementConstructor constructor,
			final ElementField[] fields, final Type[] types, final Object requiredBy) {
		return new Provider<Object>() {
			private volatile Object obj;

			public Object get() {
				if (obj == null) {
					synchronized (this) {
						if (obj == null) {
							obj = createInstance(constructor, types, requiredBy);
							injectFields(obj, element, fields, element.binding, requiredBy);
							mProviderListeners.call(InjectorImpl.this, obj, getScope(element));
						}
					}
				}
				return obj;
			}
		};
	}

	private Object createInstance(ElementConstructor constructor, Type[] types, Object requiredBy) {
		Class<?>[] params = constructor.constructor.getParameterTypes();
		Object[] args = new Object[params.length];

		for (int i = 0; i < args.length; i++)
			args[i] = getValueOrProvider(params[i], types[i], requiredBy);

		return InjectorUtils.newInstance(constructor.constructor, args);
	}

	private void injectFields(Object receiver, Element element, ElementField[] fields, Binding<?> binding,
			Object requiredBy) {
		for (ElementField field : fields) {
			if (!field.isNoInject) {
				Object value = getValueOrProvider(field.field.getType(), field.field.getGenericType(), requiredBy);
				InjectorUtils.setField(receiver, field.field, value);
			}

			mFieldListeners.call(this, receiver, getScope(element), field);
		}
	}

	private Object getValueOrProvider(Class<?> clazz, Type type, Object requiredBy) {
		Provider<?> provider = getProvider(InjectorUtils.toActualClass(type), requiredBy);
		return Provider.class.isAssignableFrom(clazz) ? provider : provider.get();
	}

	private void traverse(ElementField[] fields) {
		for (ElementField field : fields) {
			if (!field.isNoInject)
				addTraversal(field.field.getGenericType(), field.field);
		}
	}

	private void addTraversal(Type type, Object requiredBy) {
		Class<?> clazz = InjectorUtils.toActualClass(type);
		Element element = getElement(clazz);
		addTraversal(element, requiredBy);
	}

	private void addTraversal(Element element, Object requiredBy) {
		Class<?> clazz = element.clazz;
		if (element.binding == null && element.isAbstract() && clazz != Injector.class)
			throwNoFoundBinding(clazz, requiredBy);

		Required req = new Required(element, requiredBy);
		if (isInScope(element) || (mApplicationInjector == null && clazz == Injector.class))
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

	private boolean isInScope(Element element) {
		return mContext instanceof Application ^ ApplicationScoped.class != getScope(element);
	}

	private Class<? extends Annotation> getScope(Element element) {
		Class<? extends Annotation> scope;
		if (element.binding != null && (scope = element.binding.getScope()) != null)
			return scope;
		return element.getScope();
	}

	private interface ProviderProvider extends Provider<Object> {
	}

	private class ApplicationProvider implements ProviderProvider {
		private volatile boolean isInjected;

		private final Element mElement;
		private final Provider<?> mProvider;
		private final ElementField[] mFields;
		private final Binding<?> mBinding;

		ApplicationProvider(Element element, Provider<?> provider, ElementField[] fields, Binding<?> binding) {
			mElement = element;
			mProvider = provider;
			mFields = fields;
			mBinding = binding;
		}

		@Override
		public Object get() {
			if (!isInjected) {
				synchronized (this) {
					if (!isInjected) {
						injectFields(mProvider, mElement, mFields, mBinding, mProvider);
						isInjected = true;
						mProviderListeners.call(InjectorImpl.this, this, getScope(mElement));
					}
				}
			}
			return mProvider;
		}
	}

	private class DependentProvider implements ProviderProvider {
		private final Element mElement;
		private final ElementConstructor mConstructor;
		private final Type[] mTypes;
		private final ElementField[] mFields;
		private final Object mRequiredBy;

		DependentProvider(Element element, ElementConstructor constructor, Type[] types, ElementField[] fields,
				Object requiredBy) {
			mElement = element;
			mConstructor = constructor;
			mTypes = types;
			mFields = fields;
			mRequiredBy = requiredBy;
		}

		@Override
		public Object get() {
			return createJitProvider(mElement, mConstructor, mFields, mTypes, mRequiredBy);
		}
	}

	private static class Required {
		private final Element element;
		private final Object requiredBy;

		private Required(Element element, Object requiredBy) {
			this.element = element;
			this.requiredBy = requiredBy;
		}
	}
}
