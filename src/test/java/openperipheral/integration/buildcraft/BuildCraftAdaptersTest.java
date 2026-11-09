package openperipheral.integration.buildcraft;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockNote;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import buildcraft.api.facades.FacadeType;
import buildcraft.api.facades.IFacadeItem;
import buildcraft.api.tiles.IHasWork;
import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.IPipeTile.PipeType;
import buildcraft.api.transport.PipeWire;

public class BuildCraftAdaptersTest {

    @Test
    public void machine() {
        final IHasWork machine = mock(IHasWork.class);
        when(machine.hasWork()).thenReturn(true);
        Assert.assertTrue(new AdapterMachine().hasWork(machine));
    }

    @Test
    public void pipe() {
        final IPipeTile tile = mock(IPipeTile.class);
        final IPipe pipe = mock(IPipe.class);
        when(tile.getPipe()).thenReturn(pipe);
        when(pipe.hasGate(ForgeDirection.NORTH)).thenReturn(true);
        when(pipe.isWired(PipeWire.RED)).thenReturn(true);
        when(pipe.isWireActive(PipeWire.RED)).thenReturn(true);
        when(tile.getPipeType()).thenReturn(PipeType.ITEM);
        when(tile.isPipeConnected(ForgeDirection.SOUTH)).thenReturn(true);

        final AdapterPipe adapter = new AdapterPipe();
        Assert.assertTrue(adapter.hasGate(tile, ForgeDirection.NORTH));
        Assert.assertTrue(adapter.isWired(tile, PipeWire.RED));
        Assert.assertTrue(adapter.isWireActive(tile, PipeWire.RED));
        Assert.assertEquals(PipeType.ITEM, adapter.getPipeType(tile));
        Assert.assertTrue(adapter.isPipeConnected(tile, ForgeDirection.SOUTH));
    }

    @Test
    public void pipeWithoutAPipeFails() {
        final IPipeTile tile = mock(IPipeTile.class);
        try {
            new AdapterPipe().hasGate(tile, ForgeDirection.NORTH);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void facade() {
        final IFacadeItem facade = mock(IFacadeItem.class);
        final ItemStack stack = new ItemStack(new net.minecraft.item.Item());
        when(facade.getFacadeType(stack)).thenReturn(FacadeType.Basic);
        when(facade.getBlocksForFacade(stack)).thenReturn(new Block[] { new BlockNote() });
        when(facade.getMetaValuesForFacade(stack)).thenReturn(new int[] { 3 });

        final FacadeMetaProvider provider = new FacadeMetaProvider();
        final Map<String, Object> map = (Map<String, Object>) provider.getMeta(facade, stack);
        Assert.assertEquals(FacadeType.Basic, map.get("type"));

        final List<Map<String, Object>> states = (List<Map<String, Object>>) map.get("states");
        Assert.assertEquals(1, states.size());
        Assert.assertEquals(
            new BlockNote().getUnlocalizedName(),
            states.get(0)
                .get("block"));
        Assert.assertEquals(
            3,
            states.get(0)
                .get("meta"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void facadeWithMismatchedArraysDoesNotOverrun() {
        final IFacadeItem facade = mock(IFacadeItem.class);
        final ItemStack stack = new ItemStack(new net.minecraft.item.Item());
        when(facade.getBlocksForFacade(stack)).thenReturn(new Block[] { new BlockNote(), new BlockNote() });
        when(facade.getMetaValuesForFacade(stack)).thenReturn(new int[] { 1 });

        final Map<String, Object> map = (Map<String, Object>) new FacadeMetaProvider().getMeta(facade, stack);
        Assert.assertEquals(1, ((List<?>) map.get("states")).size());
    }
}
