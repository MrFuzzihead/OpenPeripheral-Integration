package openperipheral.integration.vanilla;

import openmods.integration.IIntegrationModule;
import openperipheral.integration.OpcAccess;

public class ModuleVanillaInventory implements IIntegrationModule {

    @Override
    public String name() {
        return "Vanilla CC integration module (inventory)";
    }

    @Override
    public boolean canLoad() {
        return true;
    }

    @Override
    public void load() {
        OpcAccess.adapterRegistry.register(new AdapterInventory());
    }
}
