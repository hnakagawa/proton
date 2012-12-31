package proton.inject;

import proton.inject.internal.binding.BindingsImpl;

public interface Module {
    public void configure(BindingsImpl bindingContainer);
}
