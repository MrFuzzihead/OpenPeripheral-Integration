package openperipheral.integration;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.junit.Before;
import org.junit.Test;

/**
 * The Forge {@link Configuration} constructor needs FML's launch class loader, so the migration logic is
 * exercised through mocks of the small surface it uses.
 */
public class ConfigMigrationsTest {

    private static final String CATEGORY = OpenPeripheralIntegration.CATEGORY_MODULES;

    private Configuration config;
    private ConfigCategory category;
    private Property legacy;
    private Property current;

    @Before
    public void setUp() {
        config = mock(Configuration.class);
        category = mock(ConfigCategory.class);
        legacy = mock(Property.class);
        current = mock(Property.class);

        when(config.hasCategory(CATEGORY)).thenReturn(true);
        when(config.getCategory(CATEGORY)).thenReturn(category);
        when(category.get(ConfigMigrations.LEGACY_BUILDCRAFT_TILE_KEY)).thenReturn(legacy);
        when(legacy.getBoolean()).thenReturn(false);
        when(config.get(CATEGORY, ConfigMigrations.BUILDCRAFT_TILE_KEY, true)).thenReturn(current);
    }

    @Test
    public void copiesTheLegacyValueToTheNewKey() {
        when(category.containsKey(ConfigMigrations.BUILDCRAFT_TILE_KEY)).thenReturn(false);

        ConfigMigrations.migrate(config);

        verify(current).set(false);
        verify(current).setRequiresMcRestart(true);
        verify(category).remove(ConfigMigrations.LEGACY_BUILDCRAFT_TILE_KEY);
    }

    @Test
    public void keepsAnExistingNewValue() {
        when(category.containsKey(ConfigMigrations.BUILDCRAFT_TILE_KEY)).thenReturn(true);

        ConfigMigrations.migrate(config);

        verify(current, never()).set(anyBoolean());
        verify(category).remove(ConfigMigrations.LEGACY_BUILDCRAFT_TILE_KEY);
    }

    @Test
    public void noOpWithoutTheLegacyKey() {
        when(category.get(ConfigMigrations.LEGACY_BUILDCRAFT_TILE_KEY)).thenReturn(null);

        ConfigMigrations.migrate(config);

        verify(config, never()).get(CATEGORY, ConfigMigrations.BUILDCRAFT_TILE_KEY, true);
        verify(category, never()).remove(anyString());
    }

    @Test
    public void noOpWithoutTheModulesCategory() {
        when(config.hasCategory(CATEGORY)).thenReturn(false);

        ConfigMigrations.migrate(config);

        verify(config, never()).getCategory(anyString());
    }
}
