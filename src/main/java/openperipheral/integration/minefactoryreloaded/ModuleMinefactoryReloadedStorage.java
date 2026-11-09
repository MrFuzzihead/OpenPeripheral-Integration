package openperipheral.integration.minefactoryreloaded;

import openmods.Mods;
import openmods.integration.Conditions;
import openperipheral.integration.CustomIntegrationModule;
import openperipheral.integration.OpcAccess;
import openperipheral.integration.ReflectionTargets;

public class ModuleMinefactoryReloadedStorage extends CustomIntegrationModule {

    public ModuleMinefactoryReloadedStorage() {
        super(Conditions.classExists(ReflectionTargets.MinefactoryReloaded.DEEP_STORAGE_UNIT_API));
    }

    @Override
    protected String getId() {
        return Mods.MFR + " deep storage";
    }

    @Override
    public void load() {
        OpcAccess.adapterRegistry.register(new AdapterDeepStorage());
    }
}
