package openperipheral.integration.tmechworks;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;

import tmechworks.lib.blocks.IDrawbridgeLogicBase;

public class TMechworksAdaptersTest {

    @Test
    public void drawbridge() {
        final IDrawbridgeLogicBase drawbridge = mock(IDrawbridgeLogicBase.class);
        when(drawbridge.hasExtended()).thenReturn(true);

        final AdapterDrawbridgeLogicBase adapter = new AdapterDrawbridgeLogicBase();
        Assert.assertEquals("tmechworks_drawbridge", adapter.getSourceId());
        Assert.assertTrue(adapter.hasExtended(drawbridge));
    }
}
