package openperipheral.integration.ic2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;

public class ElectricItemMetaProviderTest {

    private IElectricItemManager originalManager;

    @Before
    public void installManager() {
        originalManager = ElectricItem.manager;
        ElectricItem.manager = mock(IElectricItemManager.class);
    }

    @After
    public void restoreManager() {
        ElectricItem.manager = originalManager;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void electricItemMetadata() {
        final IElectricItem item = mock(IElectricItem.class);
        final ItemStack stack = new ItemStack(new Item());
        when(item.getTier(stack)).thenReturn(2);
        when(item.getMaxCharge(stack)).thenReturn(10000.0);
        when(item.getTransferLimit(stack)).thenReturn(128.0);
        when(item.canProvideEnergy(stack)).thenReturn(true);
        when(ElectricItem.manager.getCharge(stack)).thenReturn(2500.0);

        final Map<String, Object> map = (Map<String, Object>) new ElectricItemMetaProvider().getMeta(item, stack);
        Assert.assertEquals("electric", new ElectricItemMetaProvider().getKey());
        Assert.assertEquals(2, map.get("tier"));
        Assert.assertEquals(10000.0, map.get("maxCharge"));
        Assert.assertEquals(128.0, map.get("transferLimit"));
        Assert.assertEquals(Boolean.TRUE, map.get("canProvideEnergy"));
        Assert.assertEquals(2500.0, map.get("charge"));
    }
}
