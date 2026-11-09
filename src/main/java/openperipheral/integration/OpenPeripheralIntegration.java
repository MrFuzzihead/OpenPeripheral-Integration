package openperipheral.integration;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import openmods.config.ConfigChangeListener;
import openmods.config.properties.ConfigProcessing;
import openmods.integration.IIntegrationModule;
import openmods.integration.Integration;

@Mod(
    modid = OpenPeripheralIntegration.MOD_ID,
    name = OpenPeripheralIntegration.MOD_ID,
    version = Tags.VERSION,
    guiFactory = "openperipheral.integration.ConfigGuiFactory",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:OpenMods@[0.10.15,);required-after:OpenPeripheralApi;after:ComputerCraft@[3.3.0,);after:appliedEnergistics-2;after:IC2;after:EnderStorage;after:BuildCraft|Core;after:Forestry;after:Mystcraft;after:Railcraft;after:Thaumcraft;after:ThermalExpansion;")
public class OpenPeripheralIntegration {

    public static final String MOD_ID = "OpenPeripheralIntegration";

    public static final String CATEGORY_MODULES = "modules";

    private static boolean checkConfig(Configuration config, String value) {
        final Property property = config.get(CATEGORY_MODULES, value, true);
        property.setRequiresMcRestart(true);
        return property.getBoolean();
    }

    @Instance(MOD_ID)
    public static OpenPeripheralIntegration instance;

    private Configuration config;

    Configuration config() {
        return config;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        config = new Configuration(evt.getSuggestedConfigurationFile());

        if (config.hasCategory("integration")) {
            ConfigCategory integration = config.getCategory("integration");
            config.removeCategory(integration);
        }

        for (IntegrationModules.Entry entry : IntegrationModules.entries()) {
            if (!checkConfig(config, entry.key)) continue;

            final IIntegrationModule module = entry.create();
            if (module != null) Integration.addModule(module);
        }

        ConfigProcessing.processAnnotations(MOD_ID, config, Config.class);

        FMLCommonHandler.instance()
            .bus()
            .register(new ConfigChangeListener(MOD_ID, config));

        if (config.hasChanged()) config.save();
    }

    @EventHandler
    public void init(FMLInitializationEvent evt) {
        OpcAccess.checkApiPresent();
    }

}
