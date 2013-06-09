package proton.inject.listener;

import java.lang.annotation.Annotation;

import proton.inject.Injector;
import proton.inject.internal.Element.ElementField;
import proton.inject.internal.Element.ElementField.ElementFieldListener;
import proton.inject.util.SparseClassArray;

public class FieldListeners {
	private SparseClassArray<FieldListener> mListeners = new SparseClassArray<FieldListener>();

	public void register(Class<? extends Annotation> annClass, FieldListener listener) {
		synchronized (this) {
			mListeners.put(annClass, listener);
		}
	}

	public void unregister(Class<? extends Annotation> annClass) {
		synchronized (this) {
			mListeners.remove(annClass);
		}
	}

	public FieldListener getListener(Class<?> clazz) {
		synchronized (this) {
			return mListeners.get(clazz);
		}
	}

	public void call(Injector injector, Object receiver, Class<? extends Annotation> scope, ElementField field) {
		if (field.listeners.length == 0)
			return;
		
		ElementFieldListener[] listeners = field.listeners;
		for (ElementFieldListener listener : listeners)
			listener.listener.hear(injector, receiver, scope, field.field, listener.annotation);
	}
}
