package proton.inject;

import proton.inject.internal.binding.Bindings;
import proton.inject.listener.ProviderListeners;

public interface Module {
	public void configure(Bindings bindings, ProviderListeners providerListeners);
}
