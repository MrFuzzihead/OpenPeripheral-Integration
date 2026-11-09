package openperipheral.integration.appeng;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import openperipheral.api.adapter.method.IMultiReturn;
import openperipheral.api.converter.IConverter;
import openperipheral.api.helpers.Index;
import openperipheral.integration.OpcAccess;

public class AppEngAdaptersTest {

    private static IGridHost hostWith(IGrid grid, IStorageGrid storage) {
        final IGridHost host = mock(IGridHost.class);
        final IGridNode node = mock(IGridNode.class);
        when(host.getGridNode(ForgeDirection.UNKNOWN)).thenReturn(node);
        when(node.getGrid()).thenReturn(grid);
        when(grid.getCache(IStorageGrid.class)).thenReturn(storage);
        return host;
    }

    @Test
    public void cellStatus() {
        final IChestOrDrive drive = mock(IChestOrDrive.class);
        when(drive.getCellCount()).thenReturn(4);
        when(drive.getCellStatus(0)).thenReturn(0);
        when(drive.getCellStatus(1)).thenReturn(1);
        when(drive.getCellStatus(2)).thenReturn(2);
        when(drive.getCellStatus(3)).thenReturn(3);

        final AdapterStorage adapter = new AdapterStorage();
        Assert.assertEquals(4, adapter.getCellCount(drive));

        Assert.assertArrayEquals(
            new Object[] { 0, "missing" },
            adapter.getCellStatus(drive, Index.fromJava(0, 0))
                .getObjects());
        Assert.assertArrayEquals(
            new Object[] { 1, "green" },
            adapter.getCellStatus(drive, Index.fromJava(1, 0))
                .getObjects());
        Assert.assertArrayEquals(
            new Object[] { 2, "orange" },
            adapter.getCellStatus(drive, Index.fromJava(2, 0))
                .getObjects());
        Assert.assertArrayEquals(
            new Object[] { 3, "red" },
            adapter.getCellStatus(drive, Index.fromJava(3, 0))
                .getObjects());

        when(drive.getCellStatus(0)).thenReturn(9);
        Assert.assertArrayEquals(
            new Object[] { 9, "unknown" },
            adapter.getCellStatus(drive, Index.fromJava(0, 0))
                .getObjects());
    }

    @Test
    public void cellStatusValidatesTheSlot() {
        final IChestOrDrive drive = mock(IChestOrDrive.class);
        when(drive.getCellCount()).thenReturn(2);
        try {
            new AdapterStorage().getCellStatus(drive, Index.fromJava(2, 0));
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void availableItemsLimitAndCount() {
        final IGrid grid = mock(IGrid.class);
        final IStorageGrid storage = mock(IStorageGrid.class);
        final IMEMonitor<IAEItemStack> monitor = mock(IMEMonitor.class);
        final IItemList<IAEItemStack> list = mock(IItemList.class);
        final IGridHost host = hostWith(grid, storage);

        when(storage.getItemInventory()).thenReturn(monitor);
        when(monitor.getStorageList()).thenReturn(list);
        when(list.size()).thenReturn(3);
        final List<IAEItemStack> stacks = Arrays
            .asList(mock(IAEItemStack.class), mock(IAEItemStack.class), mock(IAEItemStack.class));
        when(list.iterator()).thenAnswer(invocation -> stacks.iterator());

        final IConverter converter = mock(IConverter.class);
        when(converter.fromJava(any())).thenAnswer(invocation -> new HashMap<String, Object>());

        final AdapterNetwork adapter = new AdapterNetwork();
        Assert.assertEquals(3, adapter.getAvailableItemCount(host));
        Assert.assertEquals(
            3,
            adapter.getAvailableItems(host, converter, null, null)
                .size());
        Assert.assertEquals(
            2,
            adapter.getAvailableItems(host, converter, null, 2)
                .size());
        // Zero means "no limit" for backwards compatibility.
        Assert.assertEquals(
            3,
            adapter.getAvailableItems(host, converter, null, 0)
                .size());
    }

    @Test
    public void availableItemsRejectsNonTableConversions() {
        final IGrid grid = mock(IGrid.class);
        final IStorageGrid storage = mock(IStorageGrid.class);
        final IMEMonitor<IAEItemStack> monitor = mock(IMEMonitor.class);
        final IItemList<IAEItemStack> list = mock(IItemList.class);
        final IGridHost host = hostWith(grid, storage);

        when(storage.getItemInventory()).thenReturn(monitor);
        when(monitor.getStorageList()).thenReturn(list);
        when(list.iterator()).thenReturn(
            Collections.singletonList(mock(IAEItemStack.class))
                .iterator());

        final IConverter converter = mock(IConverter.class);
        when(converter.fromJava(any())).thenReturn("not a table");

        try {
            new AdapterNetwork().getAvailableItems(host, converter, null, null);
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void craftingCpus() {
        final IGrid grid = mock(IGrid.class);
        final IStorageGrid storage = mock(IStorageGrid.class);
        final ICraftingGrid crafting = mock(ICraftingGrid.class);
        final ICraftingCPU cpu = mock(ICraftingCPU.class);
        final IGridHost host = hostWith(grid, storage);

        when(grid.getCache(ICraftingGrid.class)).thenReturn(crafting);
        when(crafting.getCpus()).thenReturn(com.google.common.collect.ImmutableSet.of(cpu));
        when(cpu.getName()).thenReturn("cpu");
        when(cpu.getAvailableStorage()).thenReturn(100L);
        when(cpu.getCoProcessors()).thenReturn(2);
        when(cpu.isBusy()).thenReturn(false);

        final List<Map<String, Object>> cpus = new AdapterNetwork().getCraftingCPUs(host);
        Assert.assertEquals(1, cpus.size());
        Assert.assertEquals(
            "cpu",
            cpus.get(0)
                .get("name"));
        Assert.assertEquals(
            100L,
            cpus.get(0)
                .get("storage"));
        Assert.assertEquals(
            2,
            cpus.get(0)
                .get("coprocessors"));
        Assert.assertEquals(
            Boolean.FALSE,
            cpus.get(0)
                .get("busy"));
    }

    @Test
    public void energyValues() {
        final IGrid grid = mock(IGrid.class);
        final IStorageGrid storage = mock(IStorageGrid.class);
        final IEnergyGrid energy = mock(IEnergyGrid.class);
        final IGridHost host = hostWith(grid, storage);

        when(grid.getCache(IEnergyGrid.class)).thenReturn(energy);
        when(energy.getAvgPowerInjection()).thenReturn(1.0);
        when(energy.getAvgPowerUsage()).thenReturn(2.0);
        when(energy.getIdlePowerUsage()).thenReturn(3.0);
        when(energy.getMaxStoredPower()).thenReturn(4.0);
        when(energy.getStoredPower()).thenReturn(5.0);

        final AdapterNetwork adapter = new AdapterNetwork();
        Assert.assertEquals(1.0, adapter.getAvgPowerInjection(host), 0.0001);
        Assert.assertEquals(2.0, adapter.getAvgPowerUsage(host), 0.0001);
        Assert.assertEquals(3.0, adapter.getIdlePowerUsage(host), 0.0001);
        Assert.assertEquals(4.0, adapter.getMaxStoredPower(host), 0.0001);
        Assert.assertEquals(5.0, adapter.getStoredPower(host), 0.0001);
    }

    @Test
    public void unformedGridFailsWithADescriptiveError() {
        final IGridHost host = mock(IGridHost.class);
        when(host.getGridNode(ForgeDirection.UNKNOWN)).thenReturn(null);
        try {
            new AdapterNetwork().getAvailableItemCount(host);
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(
                expected.getMessage()
                    .contains("grid node"));
        }
    }

    @Test
    public void multipleReturnHelper() {
        final IMultiReturn result = openperipheral.api.helpers.MultiReturn.wrap(1, "one");
        Assert.assertArrayEquals(new Object[] { 1, "one" }, result.getObjects());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void availableItemFormats() {
        final IGrid grid = mock(IGrid.class);
        final IStorageGrid storage = mock(IStorageGrid.class);
        final IMEMonitor<IAEItemStack> monitor = mock(IMEMonitor.class);
        final IItemList<IAEItemStack> list = mock(IItemList.class);
        final IAEItemStack aeStack = mock(IAEItemStack.class);
        final IGridHost host = hostWith(grid, storage);

        when(storage.getItemInventory()).thenReturn(monitor);
        when(monitor.getStorageList()).thenReturn(list);
        when(list.iterator()).thenAnswer(
            invocation -> Collections.singletonList(aeStack)
                .iterator());
        when(list.size()).thenReturn(1);

        final net.minecraft.item.ItemStack itemStack = new net.minecraft.item.ItemStack(new net.minecraft.item.Item());
        when(aeStack.getItemStack()).thenReturn(itemStack);

        final IConverter converter = mock(IConverter.class);
        when(converter.fromJava(any())).thenAnswer(invocation -> new HashMap<String, Object>());

        final openperipheral.api.meta.IMetaProviderProxy proxy = mock(openperipheral.api.meta.IMetaProviderProxy.class);
        final openperipheral.api.meta.IItemStackPartialMetaBuilder originalMeta = OpcAccess.itemStackMetaBuilder;
        OpcAccess.itemStackMetaBuilder = mock(openperipheral.api.meta.IItemStackPartialMetaBuilder.class);
        when(OpcAccess.itemStackMetaBuilder.createProxy(any())).thenReturn(proxy);
        try {
            final AdapterNetwork adapter = new AdapterNetwork();

            final List<?> proxied = adapter.getAvailableItems(host, converter, AdapterNetwork.ItemDetails.PROXY, null);
            Assert.assertSame(proxy, ((Map<String, Object>) proxied.get(0)).get("item"));

            final List<?> detailed = adapter.getAvailableItems(host, converter, AdapterNetwork.ItemDetails.ALL, null);
            Assert.assertSame(itemStack, ((Map<String, Object>) detailed.get(0)).get("item"));

            final List<?> plain = adapter.getAvailableItems(host, converter, AdapterNetwork.ItemDetails.NONE, null);
            Assert.assertFalse(((Map<String, Object>) plain.get(0)).containsKey("item"));
        } finally {
            OpcAccess.itemStackMetaBuilder = originalMeta;
        }
    }

    @Test
    public void itemDetailWithoutStoredItems() {
        final IGrid grid = mock(IGrid.class);
        final IStorageGrid storage = mock(IStorageGrid.class);
        final IMEMonitor<IAEItemStack> monitor = mock(IMEMonitor.class);
        final IItemList<IAEItemStack> list = mock(IItemList.class);
        final IGridHost host = hostWith(grid, storage);

        when(storage.getItemInventory()).thenReturn(monitor);
        when(monitor.getStorageList()).thenReturn(list);
        when(list.iterator()).thenReturn(
            Collections.<IAEItemStack>emptyList()
                .iterator());

        Assert.assertNull(
            new AdapterNetwork().getItemDetail(
                host,
                new openperipheral.integration.vanilla.ItemFingerprint("minecraft:stone", 0, null),
                null));
    }

    @Test
    public void interfaceCanExport() {
        final net.minecraft.tileentity.TileEntity tile = mock(
            net.minecraft.tileentity.TileEntity.class,
            org.mockito.Mockito.withSettings()
                .extraInterfaces(IGridHost.class));
        final net.minecraft.world.World world = mock(net.minecraft.world.World.class);
        when(tile.getWorldObj()).thenReturn(world);
        tile.xCoord = 0;
        tile.yCoord = 0;
        tile.zCoord = 0;

        final net.minecraft.tileentity.TileEntity neighbour = mock(
            net.minecraft.tileentity.TileEntity.class,
            org.mockito.Mockito.withSettings()
                .extraInterfaces(net.minecraft.inventory.IInventory.class));
        when(world.getTileEntity(1, 0, 0)).thenReturn(neighbour);

        final AdapterInterface adapter = new AdapterInterface();
        Assert.assertTrue(adapter.canExport((IGridHost) tile, ForgeDirection.EAST));

        when(world.getTileEntity(1, 0, 0)).thenReturn(null);
        Assert.assertFalse(adapter.canExport((IGridHost) tile, ForgeDirection.EAST));
    }
}
