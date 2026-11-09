package openperipheral.integration;

/**
 * Class names resolved reflectively because the corresponding mod APIs are {@code compileOnly}. Keeping
 * them in one place makes a rename reviewable in a single diff instead of twenty, and gives the
 * reflection-based adapters a shared vocabulary.
 */
public final class ReflectionTargets {

    private ReflectionTargets() {}

    public static final class AppEng {

        private AppEng() {}

        public static final String TILE_INTERFACE = "appeng.tile.misc.TileInterface";
    }

    public static final class CoFH {

        private CoFH() {}

        public static final String TILE_LIGHT = "cofh.thermalexpansion.block.light.TileLight";
        /** Package used by older Thermal Expansion builds. */
        public static final String TILE_LIGHT_LEGACY = "thermalexpansion.block.light.TileLight";
    }

    public static final class EnderStorage {

        private EnderStorage() {}

        public static final String TILE_FREQUENCY_OWNER = "codechicken.enderstorage.common.TileFrequencyOwner";
    }

    public static final class IC2 {

        private IC2() {}

        public static final String TILE_ELECTRIC_MACHINE = "ic2.core.block.machine.tileentity.TileEntityElectricMachine";
    }

    public static final class MinefactoryReloaded {

        private MinefactoryReloaded() {}

        public static final String DEEP_STORAGE_UNIT_API = "powercrystals.minefactoryreloaded.api.IDeepStorageUnit";
        public static final String ITEM_SAFARI_NET = "powercrystals.minefactoryreloaded.item.ItemSafariNet";

        public static final String TILE_AUTO_ANVIL = "powercrystals.minefactoryreloaded.tile.machine.TileEntityAutoAnvil";
        public static final String TILE_AUTO_DISENCHANTER = "powercrystals.minefactoryreloaded.tile.machine.TileEntityAutoDisenchanter";
        public static final String TILE_AUTO_ENCHANTER = "powercrystals.minefactoryreloaded.tile.machine.TileEntityAutoEnchanter";
        public static final String TILE_AUTO_JUKEBOX = "powercrystals.minefactoryreloaded.tile.machine.TileEntityAutoJukebox";
        public static final String TILE_AUTO_SPAWNER = "powercrystals.minefactoryreloaded.tile.machine.TileEntityAutoSpawner";
        public static final String TILE_CHRONOTYPER = "powercrystals.minefactoryreloaded.tile.machine.TileEntityChronotyper";
        public static final String TILE_CHUNK_LOADER = "powercrystals.minefactoryreloaded.tile.machine.TileEntityChunkLoader";
        public static final String TILE_EJECTOR = "powercrystals.minefactoryreloaded.tile.machine.TileEntityEjector";
        public static final String TILE_HARVESTER = "powercrystals.minefactoryreloaded.tile.machine.TileEntityHarvester";
    }

    public static final class Mystcraft {

        private Mystcraft() {}

        public static final String TILE_DESK = "com.xcompwiz.mystcraft.tileentity.TileEntityDesk";
        public static final String INVENTORY_NOTEBOOK = "com.xcompwiz.mystcraft.inventory.InventoryNotebook";
    }

    public static final class Railcraft {

        private Railcraft() {}

        public static final String TILE_BOILER_FIREBOX = "mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox";
        public static final String TILE_STEAM_TURBINE = "mods.railcraft.common.blocks.machine.alpha.TileSteamTurbine";

        public static final String I_HAS_CART = "mods.railcraft.common.plugins.buildcraft.triggers.IHasCart";
        public static final String I_HAS_WORK = "mods.railcraft.common.plugins.buildcraft.triggers.IHasWork";
        public static final String I_NEEDS_FUEL = "mods.railcraft.common.plugins.buildcraft.triggers.INeedsFuel";
        public static final String I_NEEDS_MAINTENANCE = "mods.railcraft.common.plugins.buildcraft.triggers.INeedsMaintenance";
        public static final String I_TEMPERATURE = "mods.railcraft.common.plugins.buildcraft.triggers.ITemperature";
    }

    public static final class Thaumcraft {

        private Thaumcraft() {}

        public static final String TILE_ARCANE_BORE = "thaumcraft.common.tiles.TileArcaneBore";
        public static final String ITEM_ELEMENTAL_PICKAXE = "thaumcraft.common.items.equipment.ItemElementalPickaxe";
        public static final String TILE_SENSOR = "thaumcraft.common.tiles.TileSensor";
        public static final String TILE_JAR_BRAIN = "thaumcraft.common.tiles.TileJarBrain";
        public static final String TILE_DECONSTRUCTION_TABLE = "thaumcraft.common.tiles.TileDeconstructionTable";
        public static final String TILE_JAR_FILLABLE = "thaumcraft.common.tiles.TileJarFillable";
    }
}
