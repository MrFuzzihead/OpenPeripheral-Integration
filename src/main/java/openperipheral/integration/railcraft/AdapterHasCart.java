package openperipheral.integration.railcraft;

import openperipheral.api.adapter.method.ReturnType;
import openperipheral.api.adapter.method.ScriptCallable;
import openperipheral.integration.ReflectionTargets;

public class AdapterHasCart extends BooleanReflectionAdapter {

    public AdapterHasCart() {
        super("railcraft_has_cart", ReflectionTargets.Railcraft.I_HAS_CART, "hasMinecart");
    }

    @ScriptCallable(description = "Returns true if has cart", returnTypes = ReturnType.BOOLEAN)
    public boolean hasCart(Object target) {
        return callBoolean(target);
    }
}
