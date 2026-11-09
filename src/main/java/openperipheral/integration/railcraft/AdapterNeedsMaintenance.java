package openperipheral.integration.railcraft;

import openperipheral.api.adapter.method.ReturnType;
import openperipheral.api.adapter.method.ScriptCallable;

public class AdapterNeedsMaintenance extends BooleanReflectionAdapter {

    public AdapterNeedsMaintenance() {
        super(
            "railcraft_needs_maintenance",
            "mods.railcraft.common.plugins.buildcraft.triggers.INeedsMaintenance",
            "needsMaintenance");
    }

    @ScriptCallable(description = "Returns true if needs maintenance", returnTypes = ReturnType.BOOLEAN)
    public boolean needsMaintenance(Object target) {
        return callBoolean(target);
    }
}
