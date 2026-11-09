package openperipheral.integration.minefactoryreloaded;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Assert;
import org.junit.Test;

import powercrystals.minefactoryreloaded.item.ItemSafariNet;

public class MfrAdaptersTest {

    @Test
    public void deepStorage() {
        final powercrystals.minefactoryreloaded.api.IDeepStorageUnit storage = mock(
            powercrystals.minefactoryreloaded.api.IDeepStorageUnit.class);
        final ItemStack stack = new ItemStack(new Item());
        when(storage.getStoredItemType()).thenReturn(stack);
        when(storage.getMaxStoredCount()).thenReturn(64);

        final AdapterDeepStorage adapter = new AdapterDeepStorage();
        Assert.assertSame(stack, adapter.getStoredItems(storage));
        Assert.assertEquals(64, adapter.getMaxStoredItems(storage));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void safariNetMetadata() {
        final SafariNetMetaProvider provider = new SafariNetMetaProvider();
        Assert.assertNotNull(provider.getTargetClass());
        Assert.assertEquals("safari_net", provider.getKey());

        final ItemSafariNet net = mock(ItemSafariNet.class);
        when(net.getUnlocalizedName()).thenReturn("item.mfr.safarinet.reusable");

        final ItemStack hidden = new ItemStack(net);
        final NBTTagCompound hiddenTag = new NBTTagCompound();
        hiddenTag.setBoolean("hide", true);
        hidden.setTagCompound(hiddenTag);

        final Map<String, Object> hiddenMap = (Map<String, Object>) provider.getMeta(net, hidden);
        Assert.assertEquals("reusable", hiddenMap.get("type"));
        Assert.assertEquals("?", hiddenMap.get("captured"));

        final ItemStack captured = new ItemStack(net);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "Pig");
        captured.setTagCompound(tag);

        final Map<String, Object> capturedMap = (Map<String, Object>) provider.getMeta(net, captured);
        Assert.assertEquals("Pig", capturedMap.get("captured"));
    }
}
