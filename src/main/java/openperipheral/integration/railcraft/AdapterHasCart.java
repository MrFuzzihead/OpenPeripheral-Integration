package openperipheral.integration.railcraft;

import openperipheral.api.adapter.method.ReturnType;
import openperipheral.api.adapter.method.ScriptCallable;

public class AdapterHasCart extends BooleanReflectionAdapter {

    public AdapterHasCart() {
        super("railcraft_has_cart", "mods.railcraft.common.plugins.buildcraft.triggers.IHasCart", "hasMinecart");
    }

    @ScriptCallable(description = "Returns true if has cart", returnTypes = ReturnType.BOOLEAN)
    public boolean hasCart(Object target) {
        return callBoolean(target);
    }
}
