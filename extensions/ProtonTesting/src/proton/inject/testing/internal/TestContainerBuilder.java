package proton.inject.testing.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import proton.inject.AbstractModule;
import proton.inject.DefaultModule;
import proton.inject.Module;
import proton.inject.Proton;
import proton.inject.binding.BindingBuilder;
import proton.inject.testing.ProtonTestCase;
import proton.inject.testing.TestModule;
import proton.inject.testing.TestObject;
import proton.inject.util.InjectorUtils;
import android.app.Application;

public class TestContainerBuilder {

	public void build(ProtonTestCase testCase, Module... modules) {
		Class<?> clazz = testCase.getClass();

		List<Module> moduleList = createModuleList(modules);
		List<TestComponent> componentList = new ArrayList<TestComponent>();

		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			Annotation[] anns = field.getAnnotations();
			for (Annotation ann : anns) {
				Class<? extends Annotation> annType = ann.annotationType();
				if (TestObject.class.equals(annType)) {
					TestComponent component = createComponent(field, (TestObject) ann);
					componentList.add(component);
				} else if (TestModule.class.equals(annType)) {
					Module module = createModule(field);
					if (module != null)
						moduleList.add(module);
				}
			}
		}

		if (componentList.size() > 0)
			moduleList.add(createModuleWithComponents(componentList));

		Proton.initialize((Application) testCase.getContext().getApplicationContext(),
				moduleList.toArray(new Module[moduleList.size()]));
		Proton.getInjector(testCase.getContext()).inject(testCase);
	}

	private List<Module> createModuleList(Module... modules) {
		List<Module> list = new ArrayList<Module>();
		for (Module module : modules)
			list.add(module);

		list.add(new DefaultModule());
		return list;
	}

	private TestComponent createComponent(Field field, TestObject ann) {
		Class<?> bindClass = ((TestObject) ann).value();
		if (Object.class.equals(bindClass))
			bindClass = field.getType();

		return new TestComponent(bindClass, field.getType());
	}

	private Module createModule(Field field) {
		Class<?> fieldType = field.getType();
		if (InjectorUtils.isAbstract(fieldType))
			return null;
		try {
			return (Module) InjectorUtils.newInstance(fieldType.getConstructor((Class<?>[]) null), null);
		} catch (NoSuchMethodException exp) {
			return null;
		}
	}

	private Module createModuleWithComponents(final List<TestComponent> components) {
		return new AbstractModule() {
			@SuppressWarnings("unchecked")
			@Override
			protected void configure() {
				for (TestComponent component : components) {
					BindingBuilder<Object> builder = (BindingBuilder<Object>) bind(component.bindClass);
					if (Provider.class.isAssignableFrom(component.toClass))
						builder.toProvider((Class<? extends Provider<Object>>) component.toClass);
					else
						builder.to(component.toClass);
				}
			}
		};
	}

	public void destroy(ProtonTestCase testCase) {
		Proton.destroy();
	}

	private static class TestComponent {
		final Class<?> bindClass;
		final Class<?> toClass;

		TestComponent(Class<?> bindClass, Class<?> toClass) {
			this.bindClass = bindClass;
			this.toClass = toClass;
		}
	}
}
