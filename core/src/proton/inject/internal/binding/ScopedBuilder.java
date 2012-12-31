package proton.inject.internal.binding;

import java.lang.annotation.Annotation;

public interface ScopedBuilder {
	public void in(Class<? extends Annotation> scope);
}
