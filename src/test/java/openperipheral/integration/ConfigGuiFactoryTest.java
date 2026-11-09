package openperipheral.integration;

import org.junit.Assert;
import org.junit.Test;

/**
 * The config GUI itself needs a client, but the factory contract (which screen class is exposed, and the
 * empty runtime options) is plain Java and worth pinning.
 */
public class ConfigGuiFactoryTest {

    @Test
    public void exposesTheConfigScreen() {
        final ConfigGuiFactory factory = new ConfigGuiFactory();

        Assert.assertEquals(ConfigGuiFactory.ConfigScreen.class, factory.mainConfigGuiClass());
        Assert.assertTrue(
            factory.runtimeGuiCategories()
                .isEmpty());
        Assert.assertNull(factory.getHandlerFor(null));

        factory.initialize(null);
    }
}
