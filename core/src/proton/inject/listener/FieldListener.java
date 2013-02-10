package proton.inject.listener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import proton.inject.Injector;

public interface FieldListener {
	public void hear(Injector injector, Object receiver, Field field, Annotation ann);
}
