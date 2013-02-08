package proton.inject.internal.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Provider;

import proton.inject.annotation.ApplicationScoped;
import proton.inject.annotation.ContextScoped;
import proton.inject.annotation.Dependent;

public final class InjectorUtils {
	private InjectorUtils() {
	}

	public static Class<?> toActualClass(Type type) {
		if (type instanceof ParameterizedType
				&& (((ParameterizedType) type).getRawType() == Provider.class || ((ParameterizedType) type)
						.getRawType() == javax.inject.Provider.class))
			return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
		return (Class<?>) type;
	}

	public static boolean isAbstract(Class<?> clazz) {
		return clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers());
	}

	public static Class<?> getScopeAnnotation(Class<?> clazz) {
		Annotation[] anns = clazz.getAnnotations();
		for (Annotation a : anns) {
			Class<?> annClass = a.annotationType();
			if (ApplicationScoped.class == annClass || Dependent.class == annClass || ContextScoped.class == annClass)
				return annClass;

		}
		return ContextScoped.class;
	}
}
