package proton.inject.observer.event;

import android.os.Bundle;

public class OnSaveInstanceState {
	private final Bundle mOutState;

	public OnSaveInstanceState(Bundle outState) {
		mOutState = outState;
	}

	public Bundle getOutState() {
		return mOutState;
	}
}
