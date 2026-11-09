package openperipheral.integration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.junit.Assert;
import org.junit.Test;

public class OpenPeripheralIntegrationTest {

    @Test
    public void configKeysDefaultToTrueAndRequireARestart() {
        final Configuration config = mock(Configuration.class);
        final Property property = mock(Property.class);
        when(config.get(OpenPeripheralIntegration.CATEGORY_MODULES, "test-module", true)).thenReturn(property);
        when(property.getBoolean()).thenReturn(false);

        Assert.assertFalse(OpenPeripheralIntegration.checkConfig(config, "test-module"));
        verify(property).setRequiresMcRestart(true);
    }
}
