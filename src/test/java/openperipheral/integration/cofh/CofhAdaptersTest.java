package openperipheral.integration.cofh;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import cofh.api.energy.IEnergyContainerItem;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cofh.api.inventory.IInventoryHandler;
import cofh.api.item.IAugmentItem;
import cofh.api.item.IEmpowerableItem;
import cofh.api.item.IInventoryContainerItem;
import cofh.api.tileentity.IAugmentable;
import cofh.api.tileentity.IEnergyInfo;
import cofh.api.tileentity.IReconfigurableFacing;
import cofh.api.tileentity.IReconfigurableSides;
import cofh.api.tileentity.IRedstoneControl;
import cofh.api.tileentity.IRedstoneControl.ControlMode;
import cofh.api.tileentity.ISecurable;
import cofh.api.tileentity.ISecurable.AccessMode;
import cofh.api.transport.IEnderEnergyHandler;
import cofh.api.transport.IEnderFluidHandler;
import cofh.api.transport.IEnderItemHandler;
import openperipheral.integration.cofh.energy.AdapterEnergyProvider;
import openperipheral.integration.cofh.energy.AdapterEnergyReceiver;
import openperipheral.integration.cofh.energy.EnergyMetaProvider;
import openperipheral.integration.cofh.inventory.AdapterInventoryHandler;
import openperipheral.integration.cofh.item.AugumenedItemMetaProvider;
import openperipheral.integration.cofh.item.EmpoweredItemMetaProvider;
import openperipheral.integration.cofh.item.InventoryContainerMetaProvider;
import openperipheral.integration.cofh.tileentity.AdapterAugumentable;
import openperipheral.integration.cofh.tileentity.AdapterEnergyInfo;
import openperipheral.integration.cofh.tileentity.AdapterReconfigurableFacing;
import openperipheral.integration.cofh.tileentity.AdapterReconfigurableSides;
import openperipheral.integration.cofh.tileentity.AdapterRedstoneControl;
import openperipheral.integration.cofh.tileentity.AdapterSecureTile;
import openperipheral.integration.cofh.tileentity.SecurityUtils;
import openperipheral.integration.cofh.transport.AdapterEnderEnergyAttuned;
import openperipheral.integration.cofh.transport.AdapterEnderFluidAttuned;
import openperipheral.integration.cofh.transport.AdapterEnderItemAttuned;

public class CofhAdaptersTest {

    @Test
    public void energyAdapters() {
        final IEnergyProvider provider = mock(IEnergyProvider.class);
        when(provider.getEnergyStored(ForgeDirection.UNKNOWN)).thenReturn(100);
        when(provider.getMaxEnergyStored(ForgeDirection.NORTH)).thenReturn(1000);

        final AdapterEnergyProvider adapter = new AdapterEnergyProvider();
        Assert.assertEquals(100, adapter.getEnergyStored(provider, null));
        Assert.assertEquals(1000, adapter.getMaxEnergyStored(provider, ForgeDirection.NORTH));

        final IEnergyReceiver receiver = mock(IEnergyReceiver.class);
        when(receiver.getEnergyStored(ForgeDirection.SOUTH)).thenReturn(50);
        when(receiver.getMaxEnergyStored(ForgeDirection.UNKNOWN)).thenReturn(500);

        final AdapterEnergyReceiver receiverAdapter = new AdapterEnergyReceiver();
        Assert.assertEquals(50, receiverAdapter.getEnergyStored(receiver, ForgeDirection.SOUTH));
        Assert.assertEquals(500, receiverAdapter.getMaxEnergyStored(receiver, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void energyItemMetadata() {
        final IEnergyContainerItem item = mock(IEnergyContainerItem.class);
        final ItemStack stack = new ItemStack(new Item());
        when(item.getEnergyStored(stack)).thenReturn(25);
        when(item.getMaxEnergyStored(stack)).thenReturn(100);

        final Map<String, Object> map = (Map<String, Object>) new EnergyMetaProvider().getMeta(item, stack);
        Assert.assertEquals("energy_te", new EnergyMetaProvider().getKey());
        Assert.assertEquals(25, map.get("energyStored"));
        Assert.assertEquals(100, map.get("maxEnergyStored"));
    }

    @Test
    public void inventoryHandler() {
        final IInventoryHandler handler = mock(IInventoryHandler.class);
        final ItemStack stack = new ItemStack(new Item());
        when(handler.getInventoryContents(ForgeDirection.UP)).thenReturn(Collections.singletonList(stack));
        when(handler.getSizeInventory(ForgeDirection.UP)).thenReturn(1);
        when(handler.isEmpty(ForgeDirection.UP)).thenReturn(false);
        when(handler.isFull(ForgeDirection.DOWN)).thenReturn(true);

        final AdapterInventoryHandler adapter = new AdapterInventoryHandler();
        Assert.assertEquals(
            1,
            adapter.getInventoryContents(handler, ForgeDirection.UP)
                .size());
        Assert.assertEquals(1, adapter.getSizeInventory(handler, ForgeDirection.UP));
        Assert.assertFalse(adapter.isEmpty(handler, ForgeDirection.UP));
        Assert.assertTrue(adapter.isFull(handler, ForgeDirection.DOWN));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemMetadata() {
        final IAugmentItem augment = mock(IAugmentItem.class);
        final ItemStack stack = new ItemStack(new Item());
        when(augment.getAugmentTypes(stack)).thenReturn(Collections.singleton("speed"));
        when(augment.getAugmentLevel(stack, "speed")).thenReturn(2);
        Assert.assertEquals(
            2,
            ((Map<String, Object>) new AugumenedItemMetaProvider().getMeta(augment, stack)).get("speed"));

        final IEmpowerableItem empowerable = mock(IEmpowerableItem.class);
        when(empowerable.isEmpowered(stack)).thenReturn(true);
        Assert.assertEquals(Boolean.TRUE, new EmpoweredItemMetaProvider().getMeta(empowerable, stack));

        final IInventoryContainerItem container = mock(IInventoryContainerItem.class);
        when(container.getSizeInventory(stack)).thenReturn(9);
        Assert.assertEquals(
            9,
            ((Map<String, Object>) new InventoryContainerMetaProvider().getMeta(container, stack)).get("size"));
    }

    @Test
    public void tileEntityAdapters() {
        final IAugmentable augmentable = mock(IAugmentable.class);
        when(augmentable.getAugmentSlots()).thenReturn(new ItemStack[] { null });
        when(augmentable.getAugmentStatus()).thenReturn(new boolean[] { true });
        Assert.assertEquals(1, new AdapterAugumentable().getAugumentSlots(augmentable).length);
        Assert.assertTrue(new AdapterAugumentable().getAccess(augmentable)[0]);

        final IEnergyInfo info = mock(IEnergyInfo.class);
        when(info.getInfoEnergyPerTick()).thenReturn(1);
        when(info.getInfoMaxEnergyPerTick()).thenReturn(2);
        when(info.getInfoEnergyStored()).thenReturn(3);
        when(info.getInfoMaxEnergyStored()).thenReturn(4);
        final AdapterEnergyInfo energyAdapter = new AdapterEnergyInfo();
        Assert.assertEquals(1, energyAdapter.getEnergyPerTickInfo(info));
        Assert.assertEquals(2, energyAdapter.getMaxEnergyPerTickInfo(info));
        Assert.assertEquals(3, energyAdapter.getEnergyInfo(info));
        Assert.assertEquals(4, energyAdapter.getMaxEnergyInfo(info));

        final IReconfigurableFacing facing = mock(IReconfigurableFacing.class);
        when(facing.getFacing()).thenReturn(2);
        when(facing.allowYAxisFacing()).thenReturn(true);
        when(facing.rotateBlock()).thenReturn(true);
        when(facing.setFacing(3)).thenReturn(true);
        final AdapterReconfigurableFacing facingAdapter = new AdapterReconfigurableFacing();
        Assert.assertEquals(2, facingAdapter.getFacing(facing));
        Assert.assertTrue(facingAdapter.allowYAxisFacing(facing));
        Assert.assertTrue(facingAdapter.rotateBlock(facing));
        Assert.assertTrue(facingAdapter.setFacing(facing, 3));

        final IReconfigurableSides sides = mock(IReconfigurableSides.class);
        when(sides.decrSide(0)).thenReturn(true);
        when(sides.incrSide(0)).thenReturn(true);
        when(sides.setSide(0, 1)).thenReturn(true);
        when(sides.resetSides()).thenReturn(true);
        when(sides.getNumConfig(0)).thenReturn(3);
        final AdapterReconfigurableSides sidesAdapter = new AdapterReconfigurableSides();
        Assert.assertTrue(sidesAdapter.decrSide(sides, 0));
        Assert.assertTrue(sidesAdapter.incrSide(sides, 0));
        Assert.assertTrue(sidesAdapter.setSide(sides, 0, 1));
        Assert.assertTrue(sidesAdapter.resetSides(sides));
        Assert.assertEquals(3, sidesAdapter.getNumConfig(sides, 0));

        final IRedstoneControl control = mock(IRedstoneControl.class);
        when(control.getControl()).thenReturn(ControlMode.DISABLED);
        final AdapterRedstoneControl redstone = new AdapterRedstoneControl();
        Assert.assertEquals(ControlMode.DISABLED, redstone.getRedstoneControl(control));
        redstone.setRedstoneControl(control, ControlMode.HIGH);
        verify(control).setControl(ControlMode.HIGH);
    }

    @Test
    public void secureTile() {
        final ISecurable tile = mock(ISecurable.class);
        when(tile.getOwnerName()).thenReturn("owner");
        when(tile.getAccess()).thenReturn(AccessMode.PUBLIC);

        final AdapterSecureTile adapter = new AdapterSecureTile();
        Assert.assertEquals("owner", adapter.getOwnerName(tile));
        Assert.assertEquals(AccessMode.PUBLIC, adapter.getAccess(tile));
    }

    @Test
    public void securityUtils() {
        final ISecurable publicTile = mock(ISecurable.class);
        when(publicTile.getAccess()).thenReturn(AccessMode.PUBLIC);
        Assert.assertTrue(SecurityUtils.canModify(publicTile));
        SecurityUtils.checkAccess(publicTile);

        final ISecurable privateTile = mock(ISecurable.class);
        when(privateTile.getAccess()).thenReturn(AccessMode.PRIVATE);
        Assert.assertFalse(SecurityUtils.canModify(privateTile));
        try {
            SecurityUtils.checkAccess(privateTile);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        final ISecurable broken = mock(ISecurable.class);
        when(broken.getAccess()).thenReturn(null);
        Assert.assertFalse("A null access mode must fail closed", SecurityUtils.canModify(broken));

        Assert.assertTrue(SecurityUtils.canModify(new Object()));
    }

    @Test
    public void enderAttunedAdapters() {
        final IEnderEnergyHandler energy = mock(IEnderEnergyHandler.class);
        when(energy.getChannelString()).thenReturn("energy");
        when(energy.getFrequency()).thenReturn(5);
        when(energy.canSendEnergy()).thenReturn(true);
        when(energy.canReceiveEnergy()).thenReturn(false);
        final AdapterEnderEnergyAttuned energyAdapter = new AdapterEnderEnergyAttuned();
        Assert.assertEquals("energy", energyAdapter.getChannelName(energy));
        Assert.assertEquals(5, energyAdapter.getFrequency(energy));
        Assert.assertTrue(energyAdapter.canSendEnergy(energy));
        Assert.assertFalse(energyAdapter.canReceiveEnergy(energy));
        energyAdapter.setFrequency(energy, 7);
        verify(energy).setFrequency(7);
        energyAdapter.clearFrequency(energy);
        verify(energy).clearFrequency();

        final IEnderItemHandler item = mock(IEnderItemHandler.class);
        when(item.getChannelString()).thenReturn("items");
        when(item.getFrequency()).thenReturn(3);
        when(item.canSendItems()).thenReturn(true);
        when(item.canReceiveItems()).thenReturn(true);
        final AdapterEnderItemAttuned itemAdapter = new AdapterEnderItemAttuned();
        Assert.assertEquals("items", itemAdapter.getChannelName(item));
        Assert.assertEquals(3, itemAdapter.getFrequency(item));
        Assert.assertTrue(itemAdapter.canSendItems(item));
        Assert.assertTrue(itemAdapter.canReceiveItems(item));
        itemAdapter.setFrequency(item, 4);
        verify(item).setFrequency(4);
        itemAdapter.clearFrequency(item);
        verify(item).clearFrequency();

        final IEnderFluidHandler fluid = mock(IEnderFluidHandler.class);
        when(fluid.getChannelString()).thenReturn("fluids");
        when(fluid.getFrequency()).thenReturn(6);
        when(fluid.canSendFluid()).thenReturn(true);
        when(fluid.canReceiveFluid()).thenReturn(true);
        final AdapterEnderFluidAttuned fluidAdapter = new AdapterEnderFluidAttuned();
        Assert.assertEquals("fluids", fluidAdapter.getChannelName(fluid));
        Assert.assertEquals(6, fluidAdapter.getFrequency(fluid));
        Assert.assertTrue(fluidAdapter.canSendFluid(fluid));
        Assert.assertTrue(fluidAdapter.canReceiveFluid(fluid));
        fluidAdapter.setFrequency(fluid, 7);
        verify(fluid).setFrequency(7);
        fluidAdapter.clearFrequency(fluid);
        verify(fluid).clearFrequency();
    }

    @Test
    public void enderFrequencyIsRestrictedForSecureTiles() {
        final IEnderEnergyHandler energy = mock(
            IEnderEnergyHandler.class,
            org.mockito.Mockito.withSettings()
                .extraInterfaces(ISecurable.class));
        when(((ISecurable) energy).getAccess()).thenReturn(AccessMode.PRIVATE);
        try {
            new AdapterEnderEnergyAttuned().setFrequency(energy, 1);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
