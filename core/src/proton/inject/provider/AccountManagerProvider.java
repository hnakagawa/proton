package proton.inject.provider;

import javax.inject.Inject;
import javax.inject.Provider;

import android.accounts.AccountManager;
import android.content.Context;

public class AccountManagerProvider implements Provider<AccountManager> {
	@Inject
	private Context mContext;

	@Override
	public AccountManager get() {
		return AccountManager.get(mContext);
	}
}
