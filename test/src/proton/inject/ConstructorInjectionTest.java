package proton.inject;

import javax.inject.Inject;
import javax.inject.Provider;

import android.app.Application;
import android.test.mock.MockApplication;
import junit.framework.TestCase;

public class ConstructorInjectionTest extends TestCase {
	private Application mMockApplication;
	private Injector mInjector;

	protected void setUp() throws Exception {
		super.setUp();
		mMockApplication = new MockApplication();
		Proton.initialize(mMockApplication, new DefaultModule() {
			@Override
			protected void configure() {
				super.configure();
				bind(Client.class);
				bind(Aaa.class).to(AaaImp.class);
				bind(Bbb.class).to(BbbImp.class);
				bind(Ccc.class).to(CccImp.class);
			}
		});

		mInjector = Proton.getInjector(new MockContext(mMockApplication));
	}

	@Override
	protected void tearDown() throws Exception {
		Proton.destroy();
		super.tearDown();
	}

	public void testGetInstance() {
		Client obj = mInjector.getInstance(Client.class);
		assertNotNull(obj.mAaa1);
		assertEquals(obj.mAaa1, obj.mAaa2);
		assertNotNull(((BbbImp) obj.mBbb).mCcc);

		assertNotNull(obj.mAaaProvider1);
		assertEquals(obj.mAaa1, obj.mAaaProvider1.get());
		assertEquals(obj.mAaaProvider1, obj.mAaaProvider2);
	}

	public static class Client {
		private Aaa mAaa1;
		private Aaa mAaa2;
		private Bbb mBbb;

		private Provider<Aaa> mAaaProvider1;
		private Provider<Aaa> mAaaProvider2;

		@Inject
		public Client(Aaa aaa1, Aaa aaa2, Bbb bbb, Provider<Aaa> provider1, Provider<Aaa> provider2) {
			mAaa1 = aaa1;
			mAaa2 = aaa2;
			mBbb = bbb;
			mAaaProvider1 = provider1;
			mAaaProvider2 = provider2;
		}
	}

	public interface Aaa {
	}

	public static class AaaImp implements Aaa {
	}

	public interface Bbb {
	}

	public static class BbbImp implements Bbb {
		private Ccc mCcc;

		@Inject
		public BbbImp(Ccc ccc) {
			mCcc = ccc;
		}
	}

	public interface Ccc {
	}

	public static class CccImp implements Ccc {
	}
}
