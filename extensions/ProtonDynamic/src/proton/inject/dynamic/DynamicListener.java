package proton.inject.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import proton.inject.Injector;
import proton.inject.listener.FieldListener;

public class DynamicListener implements FieldListener {
	@Override
	public void hear(Injector injector, Object receiver, Class<? extends Annotation> scope, Field field, Annotation ann) {
		

	}
}
