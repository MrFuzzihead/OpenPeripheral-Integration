package openperipheral.integration.computercraft;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Assert;
import org.junit.Test;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.turtle.items.ITurtleItem;

public class ComputerCraftProvidersTest {

    @Test
    @SuppressWarnings("unchecked")
    public void turtleMetadata() {
        final ITurtleItem turtle = mock(ITurtleItem.class);
        final ItemStack stack = new ItemStack(new Item());
        final ITurtleUpgrade upgrade = mock(ITurtleUpgrade.class);
        when(turtle.getUpgrade(stack, TurtleSide.Left)).thenReturn(upgrade);
        when(upgrade.getUnlocalisedAdjective()).thenReturn("crafty");
        when(upgrade.getType()).thenReturn(TurtleUpgradeType.Tool);
        when(turtle.getFuelLevel(stack)).thenReturn(50);

        final Map<String, Object> map = (Map<String, Object>) new TurtleMetaProvider().getMeta(turtle, stack);
        Assert.assertEquals(50, map.get("fuel"));

        final Map<String, Object> left = (Map<String, Object>) map.get("left");
        Assert.assertEquals("crafty", left.get("adjective"));
        Assert.assertEquals("Tool", left.get("type"));
        Assert.assertFalse(map.containsKey("right"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mediaMetadata() {
        final Item item = mock(
            Item.class,
            org.mockito.Mockito.withSettings()
                .extraInterfaces(IMedia.class));
        final ItemStack stack = new ItemStack(item);
        final IMedia media = (IMedia) item;
        when(media.getLabel(stack)).thenReturn("disk");
        when(media.getAudioRecordName(stack)).thenReturn("record");

        final MediaMetaProvider provider = new MediaMetaProvider();
        Assert.assertTrue(provider.canApply(item, stack));

        final Map<String, Object> map = (Map<String, Object>) provider.getMeta(item, stack);
        Assert.assertEquals("disk", map.get("label"));
        Assert.assertEquals("record", map.get("record"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void computerMetadata() {
        final IComputerItem computer = mock(IComputerItem.class);
        final ItemStack stack = new ItemStack(new Item());
        when(computer.getComputerID(stack)).thenReturn(5);
        when(computer.getLabel(stack)).thenReturn("pc");
        when(computer.getFamily(stack)).thenReturn(ComputerFamily.Advanced);

        final ComputerMetaProvider provider = new ComputerMetaProvider();
        final Map<String, Object> map = (Map<String, Object>) provider.getMeta(computer, stack);
        Assert.assertEquals(5, map.get("id"));
        Assert.assertEquals("pc", map.get("label"));
        Assert.assertEquals(ComputerFamily.Advanced, map.get("type"));

        when(computer.getComputerID(stack)).thenReturn(-1);
        final Map<String, Object> unlabeled = (Map<String, Object>) provider.getMeta(computer, stack);
        Assert.assertFalse(unlabeled.containsKey("id"));
        Assert.assertFalse(unlabeled.containsKey("label"));
        Assert.assertEquals(ComputerFamily.Advanced, unlabeled.get("type"));
    }
}
