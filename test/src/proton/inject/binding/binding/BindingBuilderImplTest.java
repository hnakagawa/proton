package proton.inject.binding.binding;

import javax.inject.Provider;

import proton.inject.ApplicationScoped;
import proton.inject.Dependent;
import proton.inject.internal.binding.BindingBuilderImpl;
import proton.inject.internal.binding.Binding;

import android.test.AndroidTestCase;

public class BindingBuilderImplTest extends AndroidTestCase {

	public void testTo() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		assertEquals(Aaa.class, b.getToClass());
		new BindingBuilderImpl<Aaa>(b).to(AaaImpl.class);
		assertEquals(AaaImpl.class, b.getToClass());
	}

	public void testToWithNull() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		try {
			new BindingBuilderImpl<Aaa>(b).to(null);
			fail();
		} catch (IllegalStateException exp) {
		}
	}

	public void testToWithScopeAnnotation() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		new BindingBuilderImpl<Aaa>(b).to(AaaImpl.class);
		assertEquals(ApplicationScoped.class, b.getScope());
	}

	public void testToProviderClassOfQextendsProviderOfT() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		new BindingBuilderImpl<Aaa>(b).toProvider(AaaProvider.class);
		assertEquals(Dependent.class, b.getScope());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testToProviderClassWithNull() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		try {
			new BindingBuilderImpl<Aaa>(b).toProvider((Class) null);
			fail();
		} catch (IllegalStateException exp) {
		}
	}

	public void testToProviderProviderOfT() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		Provider<Aaa> p = new AaaProvider();
		new BindingBuilderImpl<Aaa>(b).toProvider(p);
		assertEquals(b.getProvider(), p);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testToProviderWithNull() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		try {
			new BindingBuilderImpl<Aaa>(b).toProvider((Provider) null);
			fail();
		} catch (IllegalStateException exp) {
		}
	}

	public void testIn() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		assertNull(b.getScope());
		new BindingBuilderImpl<Aaa>(b).in(ApplicationScoped.class);
		assertEquals(ApplicationScoped.class, b.getScope());
	}

	public void testInWithNull() {
		Binding<Aaa> b = new Binding<BindingBuilderImplTest.Aaa>(Aaa.class);
		try {
			new BindingBuilderImpl<Aaa>(b).in(null);
			fail();
		} catch (IllegalStateException exp) {
		}
	}

	public static interface Aaa {
	}

	@ApplicationScoped
	public static class AaaImpl implements Aaa {
	}

	@Dependent
	public static class AaaProvider implements Provider<Aaa> {
		@Override
		public Aaa get() {
			return new AaaImpl();
		}
	}
}
