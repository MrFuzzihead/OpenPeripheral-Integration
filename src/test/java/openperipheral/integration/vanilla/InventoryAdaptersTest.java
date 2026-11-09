package openperipheral.integration.vanilla;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import openperipheral.api.adapter.IWorldPosProvider;
import openperipheral.api.architecture.IArchitecture;
import openperipheral.api.helpers.Index;
import openperipheral.api.meta.IItemStackPartialMetaBuilder;
import openperipheral.integration.OpcAccess;

public class InventoryAdaptersTest {

    private static final Item ITEM = new Item();

    private IItemStackPartialMetaBuilder originalMetaBuilder;

    @Before
    public void installMetaBuilderMock() {
        originalMetaBuilder = OpcAccess.itemStackMetaBuilder;
        OpcAccess.itemStackMetaBuilder = mock(IItemStackPartialMetaBuilder.class);
    }

    @After
    public void restoreMetaBuilder() {
        OpcAccess.itemStackMetaBuilder = originalMetaBuilder;
    }

    private static ItemStack stack(int size, int damage) {
        return new ItemStack(ITEM, size, damage);
    }

    static class FakeInventory implements IInventory {

        final ItemStack[] slots;
        boolean dirty;

        FakeInventory(int size) {
            slots = new ItemStack[size];
        }

        @Override
        public int getSizeInventory() {
            return slots.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slots[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            if (slots[slot] == null) return null;
            final ItemStack result = slots[slot].splitStack(amount);
            if (slots[slot].stackSize <= 0) slots[slot] = null;
            return result;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            slots[slot] = stack;
        }

        @Override
        public String getInventoryName() {
            return "fake";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public void markDirty() {
            dirty = true;
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return true;
        }
    }

    /** A plain tile entity inventory: {@code InventoryUtils.doubleChestFix} would need a real chest block. */
    static class FakeTileInventory extends net.minecraft.tileentity.TileEntity implements IInventory {

        final ItemStack[] slots = new ItemStack[27];
        boolean dirty;

        @Override
        public int getSizeInventory() {
            return slots.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slots[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            if (slots[slot] == null) return null;
            final ItemStack result = slots[slot].splitStack(amount);
            if (slots[slot].stackSize <= 0) slots[slot] = null;
            return result;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            slots[slot] = stack;
        }

        @Override
        public String getInventoryName() {
            return "faketile";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public void markDirty() {
            dirty = true;
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return true;
        }
    }

    static class PosInventory extends FakeInventory implements IWorldPosProvider {

        final World world;
        final int x;
        final int y;
        final int z;

        PosInventory(World world, int x, int y, int z, int size) {
            super(size);
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public World getWorld() {
            return world;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getZ() {
            return z;
        }
    }

    @Test
    public void inventoryNameAndSize() {
        final FakeInventory inventory = new FakeInventory(5);
        final AdapterInventory adapter = new AdapterInventory();

        Assert.assertEquals("fake", adapter.getInventoryName(inventory));
        Assert.assertEquals(5, adapter.getInventorySize(inventory));
    }

    @Test
    public void condenseItemsMergesAndCompactsStacks() {
        final FakeInventory inventory = new FakeInventory(4);
        inventory.slots[0] = stack(3, 0);
        inventory.slots[2] = stack(5, 0);

        new AdapterInventory().condenseItems(inventory);

        Assert.assertNotNull(inventory.slots[0]);
        Assert.assertEquals(8, inventory.slots[0].stackSize);
        Assert.assertNull(inventory.slots[1]);
        Assert.assertNull(inventory.slots[2]);
        Assert.assertTrue(inventory.dirty);
    }

    @Test
    public void swapStacksExchangesSlots() {
        final FakeInventory inventory = new FakeInventory(3);
        final ItemStack first = new ItemStack(new Item(), 1, 0);
        final ItemStack second = new ItemStack(new Item(), 2, 1);
        inventory.slots[0] = first;
        inventory.slots[2] = second;

        new AdapterInventory().swapStacks(
            inventory,
            Index.fromJava(0, 0),
            Index.fromJava(2, 0),
            ForgeDirection.UNKNOWN,
            ForgeDirection.UNKNOWN);

        // InventoryUtils.swapStacks() may copy instead of moving the exact instance; compare contents.
        Assert.assertSame(second.getItem(), inventory.slots[0].getItem());
        Assert.assertEquals(second.getItemDamage(), inventory.slots[0].getItemDamage());
        Assert.assertEquals(2, inventory.slots[0].stackSize);
        Assert.assertSame(first.getItem(), inventory.slots[2].getItem());
        Assert.assertEquals(first.getItemDamage(), inventory.slots[2].getItemDamage());
        Assert.assertEquals(1, inventory.slots[2].stackSize);
        Assert.assertTrue(inventory.dirty);
    }

    @Test
    public void swapStacksRejectsOutOfRangeSlots() {
        final FakeInventory inventory = new FakeInventory(2);
        try {
            new AdapterInventory().swapStacks(
                inventory,
                Index.fromJava(0, 0),
                Index.fromJava(2, 0),
                ForgeDirection.UNKNOWN,
                ForgeDirection.UNKNOWN);
            Assert.fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    @Test
    public void getStackInSlotReturnsRawOrProxy() {
        final FakeInventory inventory = new FakeInventory(2);
        final ItemStack stack = stack(4, 0);
        inventory.slots[1] = stack;
        final openperipheral.api.meta.IMetaProviderProxy proxy = mock(openperipheral.api.meta.IMetaProviderProxy.class);
        when(OpcAccess.itemStackMetaBuilder.createProxy(stack)).thenReturn(proxy);

        final AdapterInventory adapter = new AdapterInventory();
        Assert.assertSame(stack, adapter.getStackInSlot(inventory, Index.fromJava(1, 0), null));
        Assert.assertSame(stack, adapter.getStackInSlot(inventory, Index.fromJava(1, 0), Boolean.FALSE));
        Assert.assertSame(proxy, adapter.getStackInSlot(inventory, Index.fromJava(1, 0), Boolean.TRUE));
    }

    @Test
    public void getAllStacksSkipsEmptySlotsAndUsesArchitectureIndices() {
        final FakeInventory inventory = new FakeInventory(4);
        final ItemStack stack = stack(2, 0);
        inventory.slots[1] = stack;
        final IArchitecture architecture = mock(IArchitecture.class);
        when(architecture.createIndex(anyInt())).thenAnswer(i -> Index.fromJava((Integer) i.getArguments()[0], 0));

        final Map<Index, Object> result = new AdapterInventory().getAllStacks(inventory, architecture, null);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey(Index.fromJava(1, 0)));
        verify(OpcAccess.itemStackMetaBuilder).createProxy(stack);
    }

    @Test
    public void destroyStackClearsTheSlot() {
        final FakeInventory inventory = new FakeInventory(2);
        inventory.slots[0] = stack(1, 0);

        new AdapterInventory().destroyStack(inventory, Index.fromJava(0, 0));

        Assert.assertNull(inventory.slots[0]);
        Assert.assertTrue(inventory.dirty);
    }

    @Test
    public void expandStackIsAPassThrough() {
        final ItemStack stack = stack(1, 0);
        Assert.assertSame(stack, new AdapterInventory().expandStack(new FakeInventory(1), stack));
    }

    @Test
    public void worldInventoryPullsFromNeighbour() {
        final World world = mock(World.class);
        final PosInventory target = new PosInventory(world, 5, 6, 7, 3);
        final FakeTileInventory chest = new FakeTileInventory();
        chest.setInventorySlotContents(0, stack(10, 0));
        when(world.getTileEntity(6, 6, 7)).thenReturn(chest);

        final int moved = new AdapterWorldInventory()
            .pullItem(target, ForgeDirection.EAST, Index.fromJava(0, 0), 4, Index.fromJava(1, 0));

        Assert.assertEquals(4, moved);
        Assert.assertEquals(6, chest.getStackInSlot(0).stackSize);
        Assert.assertEquals(4, target.getStackInSlot(1).stackSize);
    }

    @Test
    public void worldInventoryPushesIntoNeighbour() {
        final World world = mock(World.class);
        final PosInventory target = new PosInventory(world, 5, 6, 7, 3);
        target.setInventorySlotContents(0, stack(10, 0));
        final FakeTileInventory chest = new FakeTileInventory();
        when(world.getTileEntity(6, 6, 7)).thenReturn(chest);

        final int moved = new AdapterWorldInventory()
            .pushItem(target, ForgeDirection.EAST, Index.fromJava(0, 0), 6, null);

        Assert.assertEquals(6, moved);
        Assert.assertEquals(4, target.getStackInSlot(0).stackSize);
        Assert.assertEquals(6, chest.getStackInSlot(0).stackSize);
    }

    @Test
    public void worldInventorySupportsPlainTileEntityTargets() {
        final World world = mock(World.class);
        final FakeTileInventory target = new FakeTileInventory();
        target.setWorldObj(world);
        target.xCoord = 5;
        target.yCoord = 6;
        target.zCoord = 7;
        target.setInventorySlotContents(0, stack(1, 0));

        final FakeTileInventory other = new FakeTileInventory();
        when(world.getTileEntity(6, 6, 7)).thenReturn(other);

        final int moved = new AdapterWorldInventory()
            .pushItem(target, ForgeDirection.EAST, Index.fromJava(0, 0), 1, null);
        Assert.assertEquals(1, moved);
        Assert.assertEquals(1, other.getStackInSlot(0).stackSize);
    }

    @Test
    public void worldInventoryValidatesMissingNeighbour() {
        final World world = mock(World.class);
        final PosInventory target = new PosInventory(world, 5, 6, 7, 3);
        target.setInventorySlotContents(0, stack(1, 0));

        try {
            new AdapterWorldInventory().pushItem(target, ForgeDirection.EAST, Index.fromJava(0, 0), 1, null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (NullPointerException | IllegalArgumentException expected) {
            // expected
        }
        verify(world).getTileEntity(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void worldInventoryRejectsInvalidDirection() {
        final World world = mock(World.class);
        final PosInventory target = new PosInventory(world, 5, 6, 7, 3);
        try {
            new AdapterWorldInventory().pullItem(target, ForgeDirection.UNKNOWN, Index.fromJava(0, 0), 1, null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        verify(world, times(0)).getTileEntity(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void worldInventoryCanApplyToTileEntities() {
        Assert.assertTrue(new AdapterWorldInventory().canApply(FakeTileInventory.class));
        Assert.assertFalse(new AdapterWorldInventory().canApply(String.class));
    }

    @Test
    public void inventoryProviderMetadataWrapsTheProvidedInventory() {
        final FakeInventory inventory = new FakeInventory(2);
        inventory.slots[0] = stack(1, 0);
        final openperipheral.api.meta.IMetaProviderProxy proxy = mock(openperipheral.api.meta.IMetaProviderProxy.class);
        when(OpcAccess.itemStackMetaBuilder.createProxy(any(ItemStack.class))).thenReturn(proxy);

        final openmods.inventory.IInventoryProvider provider = mock(openmods.inventory.IInventoryProvider.class);
        when(provider.getInventory()).thenReturn(inventory);

        @SuppressWarnings("unchecked")
        final Map<Integer, ?> metadata = (Map<Integer, ?>) new InventoryProviderMetaProvider().getMeta(provider, null);
        Assert.assertEquals(1, metadata.size());
        Assert.assertSame(proxy, metadata.get(1));
    }

    @Test
    public void worldInventoryReportsEmptyOtherSide() {
        final World world = mock(World.class);
        final PosInventory target = new PosInventory(world, 5, 6, 7, 3);
        when(world.getTileEntity(anyInt(), anyInt(), anyInt())).thenReturn(null);

        try {
            new AdapterWorldInventory().pullItem(target, ForgeDirection.EAST, Index.fromJava(0, 0), 1, null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        verify(world).getTileEntity(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void worldInventoryIgnoresAnySlotForPush() {
        final World world = mock(World.class);
        final PosInventory target = new PosInventory(world, 5, 6, 7, 1);
        target.setInventorySlotContents(0, stack(2, 0));
        final FakeTileInventory chest = new FakeTileInventory();
        when(world.getTileEntity(6, 6, 7)).thenReturn(chest);

        final int moved = new AdapterWorldInventory()
            .pushItem(target, ForgeDirection.EAST, Index.fromJava(0, 0), null, null);

        Assert.assertEquals(2, moved);
        Assert.assertEquals(2, chest.getStackInSlot(0).stackSize);
        Assert.assertNull(target.getStackInSlot(0));
    }

    @Test
    @SuppressWarnings("unused")
    public void metaBuilderMockIsUsedByInventoryMetaProvider() {
        final FakeInventory inventory = new FakeInventory(1);
        inventory.slots[0] = stack(1, 0);
        final openperipheral.api.meta.IMetaProviderProxy proxy = mock(openperipheral.api.meta.IMetaProviderProxy.class);
        when(OpcAccess.itemStackMetaBuilder.createProxy(any(ItemStack.class))).thenReturn(proxy);

        final Map<Integer, ?> metadata = InventoryMetaProvider.wrapToProxyTable(inventory);
        Assert.assertEquals(1, metadata.size());
        Assert.assertSame(proxy, metadata.get(1));
    }
}
