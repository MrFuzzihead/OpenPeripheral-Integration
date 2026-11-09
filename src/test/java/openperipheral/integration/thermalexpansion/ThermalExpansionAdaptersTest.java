package openperipheral.integration.thermalexpansion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;

import cofh.thermalexpansion.block.light.TileLight;

public class ThermalExpansionAdaptersTest {

    @Test
    public void lampColourIsForwarded() {
        final TileLight lamp = mock(TileLight.class);
        when(lamp.setColor(0x123456)).thenReturn(true);

        final AdapterTileLamp adapter = new AdapterTileLamp();
        Assert.assertNotNull(adapter.getTargetClass());
        Assert.assertTrue(adapter.setColor(lamp, 0x123456));
        verify(lamp).setColor(0x123456);
    }

    @Test
    public void lampReportsFailureFromTheTile() {
        final TileLight lamp = mock(TileLight.class);
        when(lamp.setColor(0)).thenReturn(false);
        Assert.assertFalse(new AdapterTileLamp().setColor(lamp, 0));
    }
}
