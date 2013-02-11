package proton.example;

import proton.inject.scope.ContextScoped;
import proton.inject.state.SaveState;

@ContextScoped
public class GreetingImpl implements Greeting {
	private String[] mGreets = new String[] { "Good morning", "Good afternoon", "Good evening", "Good night" };

	// This value is saved automatically
	// when the android system call onSaveInstanceState
	@SaveState
	private int mState = -1;

	@Override
	public String greet() {
		mState = mState + 1 & 3;
		return mGreets[mState];
	}
}
