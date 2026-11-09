package openperipheral.integration.railcraft;

import openperipheral.api.adapter.method.ReturnType;
import openperipheral.api.adapter.method.ScriptCallable;
import openperipheral.integration.ReflectionTargets;

public class AdapterNeedsFuel extends BooleanReflectionAdapter {

    public AdapterNeedsFuel() {
        super("railcraft_needs_fuel", ReflectionTargets.Railcraft.I_NEEDS_FUEL, "needsFuel");
    }

    @ScriptCallable(description = "Returns true if needs fuel", returnTypes = ReturnType.BOOLEAN)
    public boolean needsFuel(Object target) {
        return callBoolean(target);
    }
}
