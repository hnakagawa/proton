package proton.inject.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import proton.inject.ProvisionException;
import proton.inject.binding.Binding;
import proton.inject.internal.Element.ElementField.ElementFieldListener;
import proton.inject.listener.FieldListener;
import proton.inject.listener.FieldListeners;
import proton.inject.util.InjectorUtils;

public class Element {
	public final Class<?> clazz;

	public final Binding<?> binding;

	private int mIsAbstract = -1;

	private Class<? extends Annotation> mScope;

	private ElementConstructor mConstructor;

	private ElementField[] mFields;

	public Element(Class<?> clazz, Binding<?> binding) {
		this.clazz = clazz;
		this.binding = binding;
	}

	public boolean isAbstract() {
		if (mIsAbstract == -1)
			mIsAbstract = InjectorUtils.isAbstract(clazz) ? 1 : 0;
		return mIsAbstract != 0;
	}

	public Class<? extends Annotation> getScope() {
		if (mScope != null)
			return mScope;
		mScope = InjectorUtils.getScopeAnnotation(clazz);
		return mScope;
	}

	public ElementConstructor getElementConstructor() {
		if (mConstructor != null)
			return mConstructor;

		Constructor<?> constructor = null;
		for (Constructor<?> c : clazz.getDeclaredConstructors()) {
			if (c.getAnnotation(Inject.class) == null)
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

		mConstructor = new ElementConstructor(constructor);
		return mConstructor;
	}

	public ElementField[] getElementFields(FieldListeners fieldListeners) {
		if (mFields != null)
			return mFields;

		List<ElementField> fieldsList = new ArrayList<ElementField>();
		for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
			for (Field field : c.getDeclaredFields()) {
				Annotation[] anns = field.getAnnotations();
				List<ElementFieldListener> list = new ArrayList<ElementFieldListener>();

				boolean isNoInject = true;
				for (Annotation ann : anns) {
					if (ann.annotationType() == Inject.class)
						isNoInject = false;

					FieldListener listener = fieldListeners.getListener(ann.annotationType());
					if (listener != null)
						list.add(new ElementFieldListener(ann, listener));
				}

				if (isNoInject && list.size() == 0)
					continue;

				field.setAccessible(true);
				fieldsList.add(new ElementField(field, isNoInject, list.toArray(new ElementFieldListener[list.size()])));
			}
		}

		mFields = fieldsList.toArray(new ElementField[fieldsList.size()]);
		return mFields;
	}

	public static class ElementConstructor {
		public final Constructor<?> constructor;

		private ElementConstructor(Constructor<?> constructor) {
			this.constructor = constructor;
		}
	}

	public static class ElementField {
		public final Field field;
		public final boolean isNoInject;
		public final ElementFieldListener[] listeners;

		private ElementField(Field field, boolean isNoInject, ElementFieldListener[] listeners) {
			this.field = field;
			this.isNoInject = isNoInject;
			this.listeners = listeners;
		}

		public static class ElementFieldListener {
			public final Annotation annotation;
			public final FieldListener listener;

			private ElementFieldListener(Annotation annotation, FieldListener listener) {
				this.annotation = annotation;
				this.listener = listener;
			}

		}
	}
}
