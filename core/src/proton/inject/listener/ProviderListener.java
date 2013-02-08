package proton.inject.listener;

import android.content.Context;

public interface ProviderListener {

	public void onCreateInstance(Context context, Object instance);
}
