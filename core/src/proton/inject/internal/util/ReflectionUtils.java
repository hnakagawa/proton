package proton.inject.internal.util;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Provider;

public final class ReflectionUtils {
	private ReflectionUtils() {
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
}
