package openperipheral.integration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Guards the config-key table. The previous if-chain registered the Thaumcraft module twice; a duplicate
 * key in this list (or an accidental rename) would either load a module twice or silently reset a player's
 * existing config entry, so both properties are pinned here.
 */
public class IntegrationModulesTest {

    private static Set<String> keys() {
        final Set<String> result = new HashSet<String>();
        for (IntegrationModules.Entry entry : IntegrationModules.entries()) {
            Assert.assertNotNull("Module entry has no key", entry.key);
            Assert.assertFalse("Module entry has an empty key", entry.key.isEmpty());
            result.add(entry.key);
        }
        return result;
    }

    @Test
    public void everyKeyIsUnique() {
        final List<IntegrationModules.Entry> entries = IntegrationModules.entries();
        final Set<String> seen = new HashSet<String>();
        for (IntegrationModules.Entry entry : entries) {
            Assert.assertTrue("Duplicate module config key: " + entry.key, seen.add(entry.key));
        }
        Assert.assertEquals(entries.size(), seen.size());
    }

    @Test
    public void expectedKeysAreStillPresent() {
        // These names are user-facing config keys; renaming one silently resets it for existing users.
        final Set<String> expected = new HashSet<String>(
            Arrays.asList(
                "vanilla",
                "vanilla-inventory",
                "vanilla-inventory-manipulation",
                "cofh-api-energy",
                "cofh-api-inventory",
                "cofh-api-item",
                "cofh-api-tileentity",
                "cofh-api-transport",
                "buildcraft-api-transport",
                "buildcraft-api-tilenentity",
                "buildcraft-api-facades",
                "ic2-mod",
                "ic2-api",
                "railcraft-mod",
                "railcraft-api-carts",
                "railcraft-api-fuel",
                "thaumcraft-mod",
                "thaumcraft-api",
                "mfr-mod",
                "mfr-mod-storage",
                "computercraft-mod",
                "enderstorage-mod",
                "forestry-mod",
                "mystcraft-mod",
                "tmechworks-mod",
                "thermalexpansion-mod",
                "ae2-mod"));

        Assert.assertEquals(expected, keys());
        Assert.assertEquals(
            27,
            IntegrationModules.entries()
                .size());
    }

    @Test
    public void everyEntryCanProduceAModule() {
        // Factories are lazy; invoking the ones that only need Minecraft classes must not touch a game.
        for (IntegrationModules.Entry entry : IntegrationModules.entries()) {
            // The MFR-storage and Mystcraft factories depend on runtime mod detection; they are covered
            // by the in-game test instead.
            if ("mfr-mod-storage".equals(entry.key) || "mystcraft-mod".equals(entry.key)) continue;
            Assert.assertNotNull("Factory for " + entry.key + " returned null", entry.create());
        }
    }
}
