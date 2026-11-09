package openperipheral.integration.enderstorage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Test;

import codechicken.enderstorage.common.TileFrequencyOwner;

public class EnderStorageAdaptersTest {

    @Test
    public void frequencyIsReadFromTheTile() {
        final TileFrequencyOwner tile = mock(TileFrequencyOwner.class);
        tile.freq = 0x123;

        final AdapterFrequencyOwner adapter = new AdapterFrequencyOwner();
        Assert.assertEquals(0x123, adapter.getFrequency(tile));

        // 0x123 = colours 1, 2, 3 -> ComputerCraft bitmasks 1<<1, 1<<2, 1<<3.
        Assert.assertArrayEquals(new int[] { 2, 4, 8 }, adapter.getColors(tile));
    }

    @Test
    public void frequencyIsWrittenThroughSetFreq() {
        final TileFrequencyOwner tile = mock(TileFrequencyOwner.class);
        new AdapterFrequencyOwner().setFrequency(tile, 0x456);
        verify(tile).setFreq(0x456);
    }

    @Test
    public void frequencyMustBeInRange() {
        final TileFrequencyOwner tile = mock(TileFrequencyOwner.class);
        try {
            new AdapterFrequencyOwner().setFrequency(tile, 4096);
            Assert.fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    @Test
    public void frequencyColorsUseTheLowNibbles() {
        final TileFrequencyOwner tile = mock(TileFrequencyOwner.class);
        tile.freq = 0xFED;
        Assert
            .assertArrayEquals(new int[] { 1 << 0xF, 1 << 0xE, 1 << 0xD }, new AdapterFrequencyOwner().getColors(tile));
    }
}
