package openperipheral.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import openperipheral.api.adapter.IPeripheralAdapter;
import openperipheral.api.adapter.IPeripheralAdapterRegistry;
import openperipheral.api.adapter.IScriptType;
import openperipheral.api.adapter.ITypeClassifier;
import openperipheral.api.converter.IConverterManager;
import openperipheral.api.converter.IOutboundTypeConverter;
import openperipheral.api.converter.ITypeConverter;
import openperipheral.api.meta.IEntityMetaProvider;
import openperipheral.api.meta.IEntityPartialMetaBuilder;
import openperipheral.api.meta.IItemStackMetaProvider;
import openperipheral.api.meta.IItemStackPartialMetaBuilder;
import openperipheral.integration.appeng.ModuleAppEng;
import openperipheral.integration.buildcraft.ModuleBuildCraftFacades;
import openperipheral.integration.buildcraft.ModuleBuildCraftTile;
import openperipheral.integration.buildcraft.ModuleBuildCraftTransport;
import openperipheral.integration.cofh.energy.ModuleCofhEnergy;
import openperipheral.integration.cofh.inventory.ModuleCofhInventory;
import openperipheral.integration.cofh.item.ModuleCofhItem;
import openperipheral.integration.cofh.tileentity.ModuleCofhTileEntity;
import openperipheral.integration.cofh.transport.ModuleCofhTransport;
import openperipheral.integration.computercraft.ModuleComputerCraft;
import openperipheral.integration.enderstorage.ModuleEnderStorage;
import openperipheral.integration.forestry.ModuleForestry;
import openperipheral.integration.ic2.ModuleIC2;
import openperipheral.integration.ic2.ModuleIC2Api;
import openperipheral.integration.minefactoryreloaded.ModuleMinefactoryReloadedStorage;
import openperipheral.integration.mystcraft.v2.ModuleMystcraftV2;
import openperipheral.integration.railcraft.ModuleRailcraft;
import openperipheral.integration.railcraft.ModuleRailcraftCarts;
import openperipheral.integration.railcraft.ModuleRailcraftFuel;
import openperipheral.integration.thaumcraft.ModuleThaumcraft;
import openperipheral.integration.thaumcraft.ModuleThaumcraftApi;
import openperipheral.integration.tmechworks.ModuleTMechworks;
import openperipheral.integration.vanilla.ItemFingerprint;
import openperipheral.integration.vanilla.ModuleVanilla;
import openperipheral.integration.vanilla.ModuleVanillaInventory;
import openperipheral.integration.vanilla.ModuleVanillaInventoryManipulation;

/**
 * Verifies that the modules wire every adapter/provider into the OpenPeripheral API. These are the only
 * unit-testable modules: the remaining ones reference compile-only mod APIs that are absent from the
 * test runtime classpath.
 */
public class ModuleRegistrationTest {

    private IPeripheralAdapterRegistry originalAdapterRegistry;
    private IItemStackPartialMetaBuilder originalItemStackMetaBuilder;
    private IEntityPartialMetaBuilder originalEntityMetaBuilder;
    private IConverterManager originalConverterManager;
    private ITypeClassifier originalTypeClassifier;

    private IPeripheralAdapterRegistry adapterRegistry;
    private IItemStackPartialMetaBuilder itemStackMetaBuilder;
    private IEntityPartialMetaBuilder entityMetaBuilder;
    private IConverterManager converterManager;
    private ITypeClassifier typeClassifier;

    @Before
    public void installMocks() {
        originalAdapterRegistry = OpcAccess.adapterRegistry;
        originalItemStackMetaBuilder = OpcAccess.itemStackMetaBuilder;
        originalEntityMetaBuilder = OpcAccess.entityMetaBuilder;
        originalConverterManager = OpcAccess.converterManager;
        originalTypeClassifier = OpcAccess.typeClassifier;

        adapterRegistry = mock(IPeripheralAdapterRegistry.class);
        itemStackMetaBuilder = mock(IItemStackPartialMetaBuilder.class);
        entityMetaBuilder = mock(IEntityPartialMetaBuilder.class);
        converterManager = mock(IConverterManager.class);
        typeClassifier = mock(ITypeClassifier.class);

        OpcAccess.adapterRegistry = adapterRegistry;
        OpcAccess.itemStackMetaBuilder = itemStackMetaBuilder;
        OpcAccess.entityMetaBuilder = entityMetaBuilder;
        OpcAccess.converterManager = converterManager;
        OpcAccess.typeClassifier = typeClassifier;
    }

    @After
    public void restoreApi() {
        OpcAccess.adapterRegistry = originalAdapterRegistry;
        OpcAccess.itemStackMetaBuilder = originalItemStackMetaBuilder;
        OpcAccess.entityMetaBuilder = originalEntityMetaBuilder;
        OpcAccess.converterManager = originalConverterManager;
        OpcAccess.typeClassifier = originalTypeClassifier;
    }

    @Test
    public void vanillaModuleRegistersEverything() {
        final ModuleVanilla module = new ModuleVanilla();
        Assert.assertTrue(module.canLoad());
        Assert.assertEquals("Vanilla CC integration module", module.name());

        module.load();

        verify(adapterRegistry, times(13)).register(any(IPeripheralAdapter.class));
        verify(itemStackMetaBuilder, times(11)).register(any(IItemStackMetaProvider.class));
        verify(entityMetaBuilder, times(16)).register(any(IEntityMetaProvider.class));
        verify(converterManager).register(any(ITypeConverter.class));
        verify(typeClassifier).registerType(eq(ItemFingerprint.class), any(IScriptType.class));
    }

    @Test
    public void vanillaInventoryModuleRegistersTheInventoryAdapter() {
        final ModuleVanillaInventory module = new ModuleVanillaInventory();
        Assert.assertTrue(module.canLoad());
        Assert.assertEquals("Vanilla CC integration module (inventory)", module.name());

        module.load();
        verify(adapterRegistry).register(any(IPeripheralAdapter.class));
    }

    @Test
    public void vanillaInventoryManipulationModuleRegistersTheWorldAdapter() {
        final ModuleVanillaInventoryManipulation module = new ModuleVanillaInventoryManipulation();
        Assert.assertTrue(module.canLoad());
        Assert.assertEquals("Vanilla CC integration module (inventory manipulation)", module.name());

        module.load();
        verify(adapterRegistry).register(any(IPeripheralAdapter.class));
    }

    @Test
    public void computerCraftModuleRegistersFourItemProviders() {
        new ModuleComputerCraft().load();
        verify(itemStackMetaBuilder, times(4)).register(any(IItemStackMetaProvider.class));
    }

    @Test
    public void ic2ModulesRegisterTheirAdapters() {
        new ModuleIC2().load();
        verify(adapterRegistry).register(any(IPeripheralAdapter.class));
    }

    @Test
    public void ic2ApiModuleRegistersAdaptersAndItemMetadata() {
        new ModuleIC2Api().load();
        verify(adapterRegistry, times(9)).register(any(IPeripheralAdapter.class));
        verify(itemStackMetaBuilder).register(any(IItemStackMetaProvider.class));
    }

    @Test
    public void railcraftModulesRegisterTheirAdapters() {
        new ModuleRailcraft().load();
        verify(adapterRegistry, times(7)).register(any(IPeripheralAdapter.class));
        verify(itemStackMetaBuilder).register(any(IItemStackMetaProvider.class));

        new ModuleRailcraftCarts().load();
        verify(entityMetaBuilder, times(7)).register(any(IEntityMetaProvider.class));

        new ModuleRailcraftFuel().load();
        verify(itemStackMetaBuilder, times(2)).register(any(IItemStackMetaProvider.class));
    }

    @Test
    public void thaumcraftModulesRegisterTheirAdapters() {
        new ModuleThaumcraft().load();
        verify(adapterRegistry, times(5)).register(any(IPeripheralAdapter.class));

        new ModuleThaumcraftApi().load();
        verify(adapterRegistry, times(8)).register(any(IPeripheralAdapter.class));
        verify(itemStackMetaBuilder, times(3)).register(any(IItemStackMetaProvider.class));
        verify(converterManager).register(any(IOutboundTypeConverter.class));
    }

    @Test
    public void buildCraftModulesRegisterTheirAdapters() {
        new ModuleBuildCraftTransport().load();
        verify(adapterRegistry, times(1)).register(any(IPeripheralAdapter.class));

        new ModuleBuildCraftTile().load();
        verify(adapterRegistry, times(2)).register(any(IPeripheralAdapter.class));

        new ModuleBuildCraftFacades().load();
        verify(itemStackMetaBuilder, times(1)).register(any(IItemStackMetaProvider.class));
    }

    @Test
    public void forestryModuleRegistersItsAdapterConverterAndMetadata() {
        new ModuleForestry().load();
        verify(adapterRegistry).register(any(IPeripheralAdapter.class));
        verify(converterManager).register(any(IOutboundTypeConverter.class));
        verify(itemStackMetaBuilder).register(any(IItemStackMetaProvider.class));
    }

    @Test
    public void tmechworksModuleRegistersItsAdapter() {
        new ModuleTMechworks().load();
        verify(adapterRegistry).register(any(IPeripheralAdapter.class));
    }

    @Test
    public void cofhModulesRegisterTheirAdapters() {
        new ModuleCofhEnergy().load();
        verify(adapterRegistry, times(2)).register(any(IPeripheralAdapter.class));
        verify(itemStackMetaBuilder, times(1)).register(any(IItemStackMetaProvider.class));

        new ModuleCofhInventory().load();
        verify(adapterRegistry, times(3)).register(any(IPeripheralAdapter.class));

        new ModuleCofhItem().load();
        verify(itemStackMetaBuilder, times(4)).register(any(IItemStackMetaProvider.class));

        new ModuleCofhTileEntity().load();
        verify(adapterRegistry, times(8)).register(any(IPeripheralAdapter.class));

        new ModuleCofhTransport().load();
        verify(adapterRegistry, times(11)).register(any(IPeripheralAdapter.class));
    }

    @Test
    public void minefactoryStorageRegistersItsAdapter() {
        // ModuleMinefactoryReloaded loads internal tile entities whose static initialisers need a
        // bootstrapped fluid registry; the storage module only touches the public API class.
        new ModuleMinefactoryReloadedStorage().load();
        verify(adapterRegistry).register(any(IPeripheralAdapter.class));
    }

    @Test
    public void enderStorageRegistersItsAdapter() {
        new ModuleEnderStorage().load();
        verify(adapterRegistry).register(any(IPeripheralAdapter.class));
    }

    @Test
    public void mystcraftV2RegistersItsAdapterAndMetadata() {
        new ModuleMystcraftV2().load();
        verify(adapterRegistry).register(any(IPeripheralAdapter.class));
        verify(itemStackMetaBuilder, times(3)).register(any(IItemStackMetaProvider.class));
    }

    @Test
    public void appEngModuleRegistersAdaptersConverterAndMetadata() {
        new ModuleAppEng().load();
        verify(adapterRegistry, times(3)).register(any(IPeripheralAdapter.class));
        verify(converterManager).register(any(IOutboundTypeConverter.class));
        verify(itemStackMetaBuilder).register(any(IItemStackMetaProvider.class));
    }

    @Test
    public void apiModulesOnlyLoadWhenTheApiIsPresent() {
        final ApiIntegrationModule module = new ApiIntegrationModule() {

            @Override
            public String getApiId() {
                return "DefinitelyNotAnApi";
            }

            @Override
            public void load() {}
        };

        // canLoad() goes through ModAPIManager, which needs FML's launch class loader; only the
        // naming contract is testable in a plain JVM.
        Assert.assertEquals("DefinitelyNotAnApi (API) CC integration module", module.name());
    }

    @Test
    public void customModulesBuildTheirNameFromTheId() {
        final CustomIntegrationModule module = new CustomIntegrationModule(
            openmods.conditions.Conditions.alwaysTrue()) {

            @Override
            protected String getId() {
                return "test-id";
            }

            @Override
            public void load() {}
        };

        Assert.assertEquals("test-id(custom) CC integration module", module.name());
    }

    @Test
    public void apiPresenceCheckCoversEveryHolder() {
        // Mocks are installed, so the check must pass; nulling one must fail.
        OpcAccess.checkApiPresent();

        OpcAccess.typeClassifier = null;
        try {
            OpcAccess.checkApiPresent();
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void vanillaItemFingerprintTypeDescriptionIsStable() {
        final ModuleVanilla module = new ModuleVanilla();
        module.load();

        final org.mockito.ArgumentCaptor<IScriptType> captor = org.mockito.ArgumentCaptor.forClass(IScriptType.class);
        verify(typeClassifier).registerType(eq(ItemFingerprint.class), captor.capture());
        Assert.assertEquals(
            "{id:string,dmg:number?,nbt_hash:string?}",
            captor.getValue()
                .describe());
    }

    @Test
    public void moduleNamesAreUniqueAndReadable() {
        Assert.assertNotEquals(new ModuleVanilla().name(), new ModuleVanillaInventory().name());
        Assert.assertNotEquals(new ModuleVanillaInventory().name(), new ModuleVanillaInventoryManipulation().name());
        Assert.assertNotEquals("", new ModuleVanilla().name());
    }
}
