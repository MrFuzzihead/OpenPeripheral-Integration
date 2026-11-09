package openperipheral.integration.railcraft;

import openperipheral.api.adapter.method.ReturnType;
import openperipheral.api.adapter.method.ScriptCallable;

public class AdapterNeedsFuel extends BooleanReflectionAdapter {

    public AdapterNeedsFuel() {
        super("railcraft_needs_fuel", "mods.railcraft.common.plugins.buildcraft.triggers.INeedsFuel", "needsFuel");
    }

    @ScriptCallable(description = "Returns true if needs fuel", returnTypes = ReturnType.BOOLEAN)
    public boolean needsFuel(Object target) {
        return callBoolean(target);
    }
}
