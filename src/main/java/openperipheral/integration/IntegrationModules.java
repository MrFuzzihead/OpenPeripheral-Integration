package openperipheral.integration;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import openmods.Log;
import openmods.Mods;
import openmods.integration.IIntegrationModule;
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
import openperipheral.integration.minefactoryreloaded.ModuleMinefactoryReloaded;
import openperipheral.integration.minefactoryreloaded.ModuleMinefactoryReloadedStorage;
import openperipheral.integration.mystcraft.v1.ModuleMystcraftV1;
import openperipheral.integration.mystcraft.v2.ModuleMystcraftV2;
import openperipheral.integration.railcraft.ModuleRailcraft;
import openperipheral.integration.railcraft.ModuleRailcraftCarts;
import openperipheral.integration.railcraft.ModuleRailcraftFuel;
import openperipheral.integration.thaumcraft.ModuleThaumcraft;
import openperipheral.integration.thaumcraft.ModuleThaumcraftApi;
import openperipheral.integration.thermalexpansion.ModuleThermalExpansion;
import openperipheral.integration.tmechworks.ModuleTMechworks;
import openperipheral.integration.vanilla.ModuleVanilla;
import openperipheral.integration.vanilla.ModuleVanillaInventory;
import openperipheral.integration.vanilla.ModuleVanillaInventoryManipulation;

/**
 * Single source of truth for the config keys behind {@link OpenPeripheralIntegration}'s modules.
 *
 * <p>
 * Keeping this as a table (instead of a chain of {@code if (checkConfig(...))} statements) makes the
 * key/module mapping reviewable, keeps the registration order explicit, and lets a unit test assert that
 * no key is used twice — a duplicate key silently registers (and loads) the same module more than once.
 *
 * <p>
 * The factories are lazy on purpose: constructing some modules touches third-party classes through
 * reflection, so the table can be inspected without a game running.
 */
public final class IntegrationModules {

    public static final class Entry {

        public final String key;
        private final Supplier<IIntegrationModule> factory;

        private Entry(String key, Supplier<IIntegrationModule> factory) {
            this.key = key;
            this.factory = factory;
        }

        /** Creates the module, or returns {@code null} when the installed version is unsupported. */
        public IIntegrationModule create() {
            return factory.get();
        }
    }

    private static final List<Entry> ENTRIES = buildEntries();

    private IntegrationModules() {}

    public static List<Entry> entries() {
        return ENTRIES;
    }

    private static Entry entry(String key, Supplier<IIntegrationModule> factory) {
        return new Entry(key, factory);
    }

    private static List<Entry> buildEntries() {
        final ImmutableList.Builder<Entry> builder = ImmutableList.builder();

        builder.add(entry("vanilla", ModuleVanilla::new));
        builder.add(entry("vanilla-inventory", ModuleVanillaInventory::new));
        builder.add(entry("vanilla-inventory-manipulation", ModuleVanillaInventoryManipulation::new));

        builder.add(entry("cofh-api-energy", ModuleCofhEnergy::new));
        builder.add(entry("cofh-api-inventory", ModuleCofhInventory::new));
        builder.add(entry("cofh-api-item", ModuleCofhItem::new));
        builder.add(entry("cofh-api-tileentity", ModuleCofhTileEntity::new));
        builder.add(entry("cofh-api-transport", ModuleCofhTransport::new));

        builder.add(entry("buildcraft-api-transport", ModuleBuildCraftTransport::new));
        // Renamed from the historical "buildcraft-api-tilenentity" typo; ConfigMigrations copies the value.
        builder.add(entry("buildcraft-api-tileentity", ModuleBuildCraftTile::new));
        builder.add(entry("buildcraft-api-facades", ModuleBuildCraftFacades::new));

        builder.add(entry("ic2-mod", ModuleIC2::new));
        builder.add(entry("ic2-api", ModuleIC2Api::new));

        builder.add(entry("railcraft-mod", ModuleRailcraft::new));
        builder.add(entry("railcraft-api-carts", ModuleRailcraftCarts::new));
        builder.add(entry("railcraft-api-fuel", ModuleRailcraftFuel::new));

        builder.add(entry("thaumcraft-mod", ModuleThaumcraft::new));
        builder.add(entry("thaumcraft-api", ModuleThaumcraftApi::new));

        builder.add(entry("mfr-mod", ModuleMinefactoryReloaded::new));
        builder.add(entry("mfr-mod-storage", ModuleMinefactoryReloadedStorage::new));

        builder.add(entry("computercraft-mod", ModuleComputerCraft::new));
        builder.add(entry("enderstorage-mod", ModuleEnderStorage::new));
        builder.add(entry("forestry-mod", ModuleForestry::new));
        builder.add(entry("mystcraft-mod", IntegrationModules::createMystcraft));
        builder.add(entry("tmechworks-mod", ModuleTMechworks::new));
        builder.add(entry("thermalexpansion-mod", ModuleThermalExpansion::new));
        builder.add(entry("ae2-mod", ModuleAppEng::new));

        final List<Entry> result = builder.build();
        checkUniqueKeys(result);
        return result;
    }

    private static void checkUniqueKeys(List<Entry> entries) {
        final Set<String> keys = Sets.newHashSet();
        for (Entry entry : entries) {
            Preconditions.checkState(keys.add(entry.key), "Duplicate integration module config key: %s", entry.key);
        }
    }

    private static IIntegrationModule createMystcraft() {
        if (sameOrNewerVersion(Mods.MYSTCRAFT, "0.11.1.00")) {
            if (sameOrNewerVersion(Mods.MYSTCRAFT, "0.11.6.02")) {
                FMLInterModComms
                    .sendMessage(Mods.MYSTCRAFT, "API", "openperipheral.integration.mystcraft.v2.MystcraftAccess.init");
                return new ModuleMystcraftV2();
            }

            Log.warn("Unsupported Mystcraft version!");
            return null;
        }

        Log.warn("Using old Mystcraft integration, things may explode");
        return new ModuleMystcraftV1();
    }

    private static boolean sameOrNewerVersion(String modid, String version) {
        final ModContainer modContainer = Loader.instance()
            .getIndexedModList()
            .get(modid);
        if (modContainer == null) return true;

        final ArtifactVersion targetVersion = new DefaultArtifactVersion(version);
        final ArtifactVersion actualVersion = modContainer.getProcessedVersion();
        return actualVersion.compareTo(targetVersion) >= 0;
    }
}
