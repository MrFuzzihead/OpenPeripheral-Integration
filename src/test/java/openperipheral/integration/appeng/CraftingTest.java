package openperipheral.integration.appeng;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import openperipheral.api.architecture.IArchitectureAccess;
import openperipheral.api.converter.IConverter;

public class CraftingTest {

    @Test
    public void requesterInjectsIntoTheNetwork() {
        final IActionHost host = mock(IActionHost.class);
        final IArchitectureAccess access = mock(IArchitectureAccess.class);
        final Object requested = "requested";
        final CraftingRequester requester = new CraftingRequester(host, access, requested);

        final IAEItemStack items = mock(IAEItemStack.class);
        when(host.getActionableNode()).thenReturn(null);
        Assert.assertSame(items, requester.injectCraftedItems(null, items, Actionable.MODULATE));

        final IGridNode node = mock(IGridNode.class);
        final IGrid grid = mock(IGrid.class);
        final IStorageGrid storage = mock(IStorageGrid.class);
        final IMEMonitor<IAEItemStack> monitor = mock(IMEMonitor.class);
        final IAEItemStack remaining = mock(IAEItemStack.class);

        when(host.getActionableNode()).thenReturn(node);
        when(node.getGrid()).thenReturn(grid);
        when(grid.getCache(IStorageGrid.class)).thenReturn(storage);
        when(storage.getItemInventory()).thenReturn(monitor);
        when(monitor.injectItems(eq(items), eq(Actionable.MODULATE), any())).thenReturn(remaining);

        Assert.assertSame(remaining, requester.injectCraftedItems(null, items, Actionable.MODULATE));
    }

    @Test
    public void requesterBroadcastsStateChanges() {
        final IActionHost host = mock(IActionHost.class);
        final IArchitectureAccess access = mock(IArchitectureAccess.class);
        final CraftingRequester requester = new CraftingRequester(host, access, "requested");

        final ICraftingLink link = mock(ICraftingLink.class);
        when(link.isCanceled()).thenReturn(false);
        requester.jobStateChange(link);
        verify(access).signal("crafting_state", "requested", "done");

        when(link.isCanceled()).thenReturn(true);
        requester.jobStateChange(link);
        verify(access).signal("crafting_state", "requested", "canceled");

        Assert.assertTrue(
            requester.getRequestedJobs()
                .isEmpty());
    }

    @Test
    public void requesterForwardsTheGridNodeCalls() {
        final IActionHost host = mock(IActionHost.class);
        final IGridNode node = mock(IGridNode.class);
        when(host.getActionableNode()).thenReturn(node);
        when(host.getGridNode(ForgeDirection.NORTH)).thenReturn(node);
        when(host.getCableConnectionType(ForgeDirection.NORTH)).thenReturn(AECableType.SMART);

        final CraftingRequester requester = new CraftingRequester(host, mock(IArchitectureAccess.class), "x");
        Assert.assertSame(node, requester.getActionableNode());
        Assert.assertSame(node, requester.getGridNode(ForgeDirection.NORTH));
        Assert.assertEquals(AECableType.SMART, requester.getCableConnectionType(ForgeDirection.NORTH));

        requester.securityBreak();
        verify(host).securityBreak();
    }

    @Test
    public void callbackSubmitsACompletedJob() {
        final IArchitectureAccess access = mock(IArchitectureAccess.class);
        final IConverter converter = mock(IConverter.class);
        when(converter.fromJava(any())).thenReturn("converted");

        final ICraftingGrid grid = mock(ICraftingGrid.class);
        final IMEMonitor<IAEItemStack> monitor = mock(IMEMonitor.class);
        final IActionHost host = mock(IActionHost.class);
        final ICraftingCPU cpu = mock(ICraftingCPU.class);
        final IAEItemStack stack = mock(IAEItemStack.class);

        final CraftingCallback callback = new CraftingCallback(access, converter, grid, monitor, host, cpu, stack);
        final ICraftingJob job = mock(ICraftingJob.class);
        when(job.isSimulation()).thenReturn(false);

        callback.calculationComplete(job);

        verify(grid).submitJob(eq(job), any(CraftingRequester.class), eq(cpu), eq(false), any());
        verify(converter).fromJava(stack);
    }
}
