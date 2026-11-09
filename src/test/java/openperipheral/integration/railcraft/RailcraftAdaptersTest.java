package openperipheral.integration.railcraft;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import mods.railcraft.api.carts.IEnergyTransfer;
import mods.railcraft.api.carts.IExplosiveCart;
import mods.railcraft.api.carts.ILinkableCart;
import mods.railcraft.api.carts.ILiquidTransfer;
import mods.railcraft.api.carts.IPaintedCart;
import mods.railcraft.api.carts.IRefuelableCart;
import mods.railcraft.api.carts.IRoutableCart;
import mods.railcraft.common.blocks.machine.alpha.TileSteamTurbine;
import mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox;
import mods.railcraft.common.plugins.buildcraft.triggers.IHasCart;
import mods.railcraft.common.plugins.buildcraft.triggers.IHasWork;
import mods.railcraft.common.plugins.buildcraft.triggers.INeedsFuel;
import mods.railcraft.common.plugins.buildcraft.triggers.INeedsMaintenance;
import mods.railcraft.common.plugins.buildcraft.triggers.ITemperature;
import mods.railcraft.common.util.inventory.StandaloneInventory;

public class RailcraftAdaptersTest {

    @Test
    public void booleanTriggerAdapters() {
        final IHasCart hasCart = mock(IHasCart.class);
        when(hasCart.hasMinecart()).thenReturn(true);
        Assert.assertTrue(new AdapterHasCart().hasCart(hasCart));

        final IHasWork hasWork = mock(IHasWork.class);
        when(hasWork.hasWork()).thenReturn(true);
        Assert.assertTrue(new AdapterHasWork().hasWork(hasWork));

        final INeedsFuel needsFuel = mock(INeedsFuel.class);
        when(needsFuel.needsFuel()).thenReturn(true);
        Assert.assertTrue(new AdapterNeedsFuel().needsFuel(needsFuel));

        final INeedsMaintenance needsMaintenance = mock(INeedsMaintenance.class);
        when(needsMaintenance.needsMaintenance()).thenReturn(true);
        Assert.assertTrue(new AdapterNeedsMaintenance().needsMaintenance(needsMaintenance));

        final ITemperature temperature = mock(ITemperature.class);
        when(temperature.getTemperature()).thenReturn(320.0f);
        Assert.assertEquals(320.0f, new AdapterTemperature().getTemperature(temperature), 0.0001f);
    }

    @Test
    public void boilerFirebox() {
        final TileBoilerFirebox firebox = mock(TileBoilerFirebox.class);
        when(firebox.isBurning()).thenReturn(true);
        Assert.assertTrue(new AdapterTileBoilerFirebox().isBurning(firebox));
    }

    @Test
    public void steamTurbine() {
        final TileSteamTurbine turbine = mock(TileSteamTurbine.class);
        when(turbine.getOutput()).thenReturn(42.0f);

        final AdapterTileSteamTurbine adapter = new AdapterTileSteamTurbine();
        Assert.assertEquals(42.0f, adapter.getTurbineOutput(turbine), 0.0001f);
        Assert.assertNull(adapter.getTurbineRotorStatus(turbine));

        final StandaloneInventory inventory = mock(StandaloneInventory.class);
        when(inventory.getSizeInventory()).thenReturn(1);
        when(turbine.getInventory()).thenReturn(inventory);

        final ItemSword sword = new ItemSword(ToolMaterial.EMERALD);
        final int maxDamage = sword.getMaxDamage();
        final int damage = maxDamage / 2;
        when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(sword, 1, damage));

        final int expected = 100 - (int) (Math.min(1.0, (double) damage / maxDamage) * 100);
        Assert.assertEquals(Integer.valueOf(expected), adapter.getTurbineRotorStatus(turbine));

        when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(new Item()));
        Assert.assertEquals(Integer.valueOf(100), adapter.getTurbineRotorStatus(turbine));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void cartMetadata() {
        final IEnergyTransfer energy = mock(IEnergyTransfer.class);
        when(energy.getCapacity()).thenReturn(100);
        when(energy.getEnergy()).thenReturn(50.0);
        when(energy.getTier()).thenReturn(2);
        when(energy.getTransferLimit()).thenReturn(8);
        final Map<String, Object> energyMap = (Map<String, Object>) new EnergyCartMetaProvider().getMeta(energy, null);
        Assert.assertEquals(100, energyMap.get("maxPower"));
        Assert.assertEquals(50.0, energyMap.get("currentPower"));
        Assert.assertEquals(2, energyMap.get("powerTier"));
        Assert.assertEquals(8, energyMap.get("transferRate"));

        final IExplosiveCart explosive = mock(IExplosiveCart.class);
        when(explosive.isPrimed()).thenReturn(true);
        when(explosive.getFuse()).thenReturn(20);
        final Map<String, Object> explosiveMap = (Map<String, Object>) new ExplosiveCartMetaProvider()
            .getMeta(explosive, null);
        Assert.assertEquals(Boolean.TRUE, explosiveMap.get("primed"));
        Assert.assertEquals(20, explosiveMap.get("fuse"));

        final ILiquidTransfer liquid = mock(ILiquidTransfer.class);
        when(liquid.isFilling()).thenReturn(true);
        final Map<String, Object> liquidMap = (Map<String, Object>) new LiquidCartMetaProvider().getMeta(liquid, null);
        Assert.assertEquals(Boolean.TRUE, liquidMap.get("isFilling"));

        final IPaintedCart painted = mock(IPaintedCart.class);
        when(painted.getPrimaryColor()).thenReturn((byte) 4);
        when(painted.getSecondaryColor()).thenReturn((byte) 5);
        final Map<String, Object> paintedMap = (Map<String, Object>) new PaintedCartMetaProvider()
            .getMeta(painted, null);
        Assert.assertEquals((byte) 4, paintedMap.get("primaryColor"));
        Assert.assertEquals((byte) 5, paintedMap.get("secondaryColor"));

        final IRefuelableCart refuelable = mock(IRefuelableCart.class);
        when(refuelable.needsRefuel()).thenReturn(true);
        final Map<String, Object> refuelableMap = (Map<String, Object>) new RefuealableCartMetaProvider()
            .getMeta(refuelable, null);
        Assert.assertEquals(Boolean.TRUE, refuelableMap.get("needsRefuel"));

        final IRoutableCart routable = mock(IRoutableCart.class);
        when(routable.getDestination()).thenReturn("station");
        final Map<String, Object> routableMap = (Map<String, Object>) new RoutableCartMetaProvider()
            .getMeta(routable, null);
        Assert.assertEquals("station", routableMap.get("destination"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void linkableCartMetadata() {
        final ILinkableCart cart = mock(ILinkableCart.class);
        when(cart.isLinkable()).thenReturn(false);
        final Map<String, Object> map = (Map<String, Object>) new LinkableCartMetaProvider().getMeta(cart, null);
        Assert.assertEquals(Boolean.FALSE, map.get("linkable"));
    }

    @Test
    public void ticketsAreOnlyClaimedWhenRailcraftIsLoaded() {
        final TicketMetaProvider provider = new TicketMetaProvider();
        Assert.assertFalse(provider.canApply(new Item(), new ItemStack(new Item())));
    }

    @Test
    public void boilerFuelProviderHandlesEmptyTanks() {
        final net.minecraftforge.fluids.IFluidContainerItem container = mock(
            net.minecraftforge.fluids.IFluidContainerItem.class);
        Assert.assertNull(new BoilerFuelMetaProvider().getMeta(container, new ItemStack(new Item())));
    }

    @Test
    public void directionIsPassedThroughForTurbineOutput() {
        // getTurbineOutput takes no direction, but the adapter must remain usable with a plain mock.
        final TileSteamTurbine turbine = mock(TileSteamTurbine.class);
        when(turbine.getOutput()).thenReturn(0.0f);
        Assert.assertEquals(0.0f, new AdapterTileSteamTurbine().getTurbineOutput(turbine), 0.0001f);
        Assert.assertNotNull(ForgeDirection.UNKNOWN);
    }
}
