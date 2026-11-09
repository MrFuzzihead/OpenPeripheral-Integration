package openperipheral.integration.railcraft;

import openperipheral.api.adapter.method.ReturnType;
import openperipheral.api.adapter.method.ScriptCallable;

public class AdapterHasWork extends BooleanReflectionAdapter {

    public AdapterHasWork() {
        super("railcraft_has_work", "mods.railcraft.common.plugins.buildcraft.triggers.IHasWork", "hasWork");
    }

    @ScriptCallable(description = "Returns true if has work", returnTypes = ReturnType.BOOLEAN)
    public boolean hasWork(Object target) {
        return callBoolean(target);
    }
}
