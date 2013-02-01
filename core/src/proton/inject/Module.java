package proton.inject;

import proton.inject.internal.binding.Bindings;

public interface Module {
    public void configure(Bindings bindings);
}
