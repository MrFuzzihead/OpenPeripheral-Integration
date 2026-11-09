package openperipheral.integration.railcraft;

import openperipheral.api.adapter.method.ReturnType;
import openperipheral.api.adapter.method.ScriptCallable;
import openperipheral.integration.ReflectionTargets;

public class AdapterHasWork extends BooleanReflectionAdapter {

    public AdapterHasWork() {
        super("railcraft_has_work", ReflectionTargets.Railcraft.I_HAS_WORK, "hasWork");
    }

    @ScriptCallable(description = "Returns true if has work", returnTypes = ReturnType.BOOLEAN)
    public boolean hasWork(Object target) {
        return callBoolean(target);
    }
}
