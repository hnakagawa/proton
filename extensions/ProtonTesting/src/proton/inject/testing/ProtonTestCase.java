package proton.inject.testing;

import proton.inject.Module;
import proton.inject.testing.internal.TestContainerBuilder;

import android.content.Context;
import android.test.AndroidTestCase;

public class ProtonTestCase extends AndroidTestCase {
	private TestContainerBuilder mTestContainerBuilder;

	@Override
	protected void setUp() throws Exception {
		this.setUp(null, new Module[0]);
	}

	protected void setUp(Context context) throws Exception {
		setUp(context, new Module[0]);
	}

	protected void setUp(Module... modules) throws Exception {
		setUp(null, modules);
	}

	protected void setUp(Context context, Module... modules) throws Exception {
		super.setUp();

		if (context != null)
			setContext(context);

		mTestContainerBuilder = new TestContainerBuilder();
		mTestContainerBuilder.build(this, modules);
	}

	@Override
	protected void tearDown() throws Exception {
		mTestContainerBuilder.destroy(this);
		super.tearDown();
	}
}
