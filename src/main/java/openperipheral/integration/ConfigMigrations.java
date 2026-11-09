package openperipheral.integration;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import openmods.Log;

/**
 * One-off config migrations. Keys are user-visible and persisted, so a rename must keep the old value
 * instead of silently resetting it when the config is first loaded after an update.
 */
public final class ConfigMigrations {

    static final String LEGACY_BUILDCRAFT_TILE_KEY = "buildcraft-api-tilenentity";
    static final String BUILDCRAFT_TILE_KEY = "buildcraft-api-tileentity";

    private ConfigMigrations() {}

    public static void migrate(Configuration config) {
        migrateKey(config, LEGACY_BUILDCRAFT_TILE_KEY, BUILDCRAFT_TILE_KEY);
    }

    private static void migrateKey(Configuration config, String legacyKey, String currentKey) {
        if (!config.hasCategory(OpenPeripheralIntegration.CATEGORY_MODULES)) return;

        final ConfigCategory category = config.getCategory(OpenPeripheralIntegration.CATEGORY_MODULES);
        final Property legacy = category.get(legacyKey);
        if (legacy == null) return;

        if (!category.containsKey(currentKey)) {
            final Property current = config.get(OpenPeripheralIntegration.CATEGORY_MODULES, currentKey, true);
            current.set(legacy.getBoolean());
            current.setRequiresMcRestart(true);
        }

        category.remove(legacyKey);
        Log.info("Migrated config option %s -> %s", legacyKey, currentKey);
    }
}
